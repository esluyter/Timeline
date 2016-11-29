EnvelopeClipView : ClipView {
  var env_mouse, env_nodes, env_curve, mouse_action_called = false;

  refreshEnv {
    view.setEnv(clip.env);
    view.strings_( clip.env.levels.collect(_.asStringPrec(3)) );
  }

  init { |parent, argclip, argscale, argheight, argtop, argcolor, argfgcolor|
    timelineView = parent;
    clip = argclip;
    //scale = argscale;
    height = argheight;
    top = argtop;
    color = argcolor;
    fgcolor = argfgcolor;

    this.view = EnvelopeView(parent)
    .setEnv(clip.env)
    .thumbSize_(12)
    .keepHorizontalOrder_(true)
    .strokeColor_(fgcolor)
    .fillColor_(fgcolor)
    .background_(color)
    .gridColor_(Color.hsv(0, 0, 0, 0))
    .focusColor_(Color.hsv(0, 0, 0, 0))
    .action_({ |view|
      var levels, times;
      #times, levels = view.value;
      clip.start = clip.start + (times[0] * clip.env.totalDuration * timelineView.timeline.tempo);
      clip.env = Env(levels, times.differentiate.drop(1) * clip.env.totalDuration, clip.env.curves);

      this.refreshEnv;

      this.makeBounds;
      mouse_action_called = true;
    })
    .beginDragAction_({ |view, x, y|
      //dragfrom = x@y;
      oldbounds = this.bounds;
      if (mousedownbypass) { // hack to get around cmd-drag bypassing .mouseDownAction
        dragconstrain = true;
      } {
        dragconstrain = false;
      };
      this;
    })
    .mouseDownAction_({ |view, x, y, mod, buttonnumber, clickcount|

      var scaledx = x / view.bounds.width;
      var nodeabove, nodebelow;

      dragfrom = x@y;

      view.value[0].do { |nodex, i|
        if ((nodex > scaledx) && (nodeabove.isNil)) {
          nodeabove = i;
          nodebelow = i - 1;
        };
      };

      env_nodes = [nodebelow, nodeabove];
      env_mouse = x@y;

      if ((clickcount == 2) && (mouse_action_called.not) && (mod.isShift || mod.isAlt || editing)) {
        var time = scaledx * clip.env.totalDuration;

        var levels = clip.env.levels;
        var level = clip.env[time];
        var times = clip.env.times;

        var curves = if (clip.env.curves.isArray) { clip.env.curves } { clip.env.curves ! times.size };
        var curve = curves[nodebelow];

        var existingtime = times[nodebelow];
        var timebefore = times[-1..(-1 + nodebelow)].sum;
        var newtime_a = time - timebefore;
        var newtime_b = existingtime - newtime_a;

        times = times.insert(nodebelow, newtime_a);
        times[nodeabove] = newtime_b;
        levels = levels.insert(nodeabove, level);
        curves = curves.insert(nodebelow, curve);

        clip.env = Env(levels, times, curves);
        this.refreshEnv;
        mouse_action_called = true;
      } {
        if (buttonnumber == 1) {
          this.makeContextMenu(x, y);
        } {
          if (mod.isAlt && mod.isShift.not) {
            this.copy;
          } {

            if (mod.isShift) {
              if (mod.isAlt) {
                timelineView.removeSelection(this);
              } {
                timelineView.addSelection(this);
                dragSelected = true;
              };
            } {
              if (selected.not) {
                timelineView.select(this)
              } {
                dragSelected = true;
              };
            };

            if ((clickcount > 1) && (editing.not)) {
              this.edit;
            };

            if ((y < 5) || (y > (height - 5))) {
              dragHeight = true;
              dragSide = if (y < 5) { \top } { \bottom };
            } {
              if (x < 5) {
                dragWidth = true;
                dragSide = \left;
              };
              if (x > (view.bounds.width - 5)) {
                dragWidth = true;
                dragSide = \right;
              }
            };


          };
        };
      };

      nil;
    })
    .mouseMoveAction_({ |view, x, y, mod|
      var editCurves = (mod.isAlt || mod.isShift);
      var editPoint = mouse_action_called;

      if (dragging.not && editCurves.not && editPoint.not && (dist(x@y, dragfrom) > 3)) {
        mousedownbypass = false; // hack to get around cmd-drag bypassing this method
        view.beginDrag(x, y);
      };

      if (editCurves && mouse_action_called.not) {
        //adjust curves
        var levels = clip.env.levels;
        var times = clip.env.times;
        var curves = if (clip.env.curves.isArray) { clip.env.curves } { clip.env.curves ! times.size };
        var curve = curves[env_nodes[0]]; // get curve under mouse
        var adjustment = (env_mouse.y - y) * 0.2;

        if (curve.isSymbol) { curve = 0.0 };
        if (env_curve.isNil) { env_curve = curve; }; // save curve before adjustment
        if (levels[env_nodes[0]] < levels[env_nodes[1]]) {
          curve = env_curve - adjustment;
        } {
          curve = env_curve + adjustment;
        };

        view.setEnv(clip.env.curves_(curves.put(env_nodes[0], curve)));
      };

    })
    .mouseUpAction_({
      env_curve = nil;
      mouse_action_called = false;
      this.releasedrag;
    })
    .keyDownAction_({ |view, char, mod, unicode, keycode, key|

      if (unicode == 127) { // delete
        if (view.index > -1) {
          var idx = view.index;

          var times = clip.env.times;
          var levels = clip.env.levels;
          var curves = if (clip.env.curves.isArray) { clip.env.curves } { clip.env.curves ! times.size };

          var totaltime = times[idx] + times[idx - 1];

          curves.removeAt(idx);
          levels.removeAt(idx);
          times.removeAt(idx);
          times[idx - 1] = totaltime;


          clip.env = Env(levels, times, curves);
          this.refreshEnv;
        };
      } {
        timelineView.handlekey(view, char, mod, unicode, keycode, key);
      };

    });

    this.canReceiveDragHandler_({|view, x, y|
      if (View.currentDrag == this) {
        var diff = x@y - dragfrom;
        var mousecoord, viewcoord, parentbounds;

        //this.dragbounds(this.bounds.copy.top_(this.bounds.top + diff.y).left_(this.bounds.left + diff.x));
        this.dragbounds(diff);

        // find if mouse is within 50px of the edge
        parentbounds = view.parent.bounds;
        viewcoord = this.bounds.left@this.bounds.top - view.parent.visibleOrigin;
        mousecoord = viewcoord + (x@y);

        if (parentbounds.width - mousecoord.x < 50) {
          view.parent.visibleOrigin_(view.parent.visibleOrigin + (50@0));
        };
        if (mousecoord.x < 50) {
          view.parent.visibleOrigin_(view.parent.visibleOrigin - (50@0));
        };

        if (parentbounds.height - mousecoord.y < 50) {
          view.parent.visibleOrigin_(view.parent.visibleOrigin + (0@50));
        };
        if (mousecoord.y < 50) {
          view.parent.visibleOrigin_(view.parent.visibleOrigin - (0@50));
        };

        true;
      } {
        true;
      };
    });
    this.receiveDragHandler_({ |view, x, y|
      if (View.currentDrag == this) {
        var diff = x@y - dragfrom;
        //this.dragbounds(this.bounds.copy.top_(this.bounds.top + diff.y).left_(this.bounds.left + diff.x));
        this.dragbounds(diff);
      };
      this.releasedrag;
    });


    this.refreshEnv;

    this.visualinitstuff;//.draginitstuff;
    this.makeBounds;

  }

  makeBounds {
    this.bounds = Rect(clip.start * timelineView.scale, top, clip.dur * timelineView.scale * timelineView.timeline.tempo, height);
    border.bounds = this.bounds.copy.origin_(0@0);
  }

  makeEditView { |parent, bounds|
    var v = View(parent, bounds).background_(Color.red);

    ^v;
  }

  makeContextMenu { |x, y|
    ContextMenu.create(this, x, y,
      {"Edit clip"}, {
        this.edit;

      },
      "Rename clip", { this.rename },
      "Remove from Timeline", { this.remove },
      "", {},
      "Insert envelope code in current Document", { Document.current.selectedString_(clip.startfunc.def.sourceCode) },
    );
  }

  makeEditPanel { |parent, bounds|
    ^EnvelopeClipEditPanel(parent, bounds, clip);
  }

  fgcolor_ { |newcolor|
    fgcolor = newcolor;
    namegui.stringColor_(fgcolor);
    view.strokeColor_(fgcolor);
    view.fillColor_(fgcolor);
  }

  copy {
    //^EnvelopeClipView(this.parent, clip, nil, height, top + height + 10);
  }
}










EnvelopeParamView : SCViewHolder {
  var parent, clip, clipView, timelineView, param, top, height, color, fgcolor;
  var label;

  *new { |parent, clipView, param, top, height, color, fgcolor|
    ^super.new.init(parent, clipView, param, top, height, color, fgcolor);
  }

  init { |argparent, argclipView, argparam, argtop, argheight, argcolor, argfgcolor|
    parent = argparent;
    clipView = argclipView;
    timelineView = argclipView.timelineView;
    clip = clipView.clip;
    param = argparam;
    top = argtop;
    height = argheight;
    color = argcolor;
    fgcolor = argfgcolor;

    this.view = EnvelopeView(parent)
    .setEnv(clip.perform(param))
    .thumbSize_(12)
    .keepHorizontalOrder_(true)
    .strokeColor_(fgcolor)
    .fillColor_(fgcolor)
    .background_(color)
    .gridColor_(Color.hsv(0, 0, 0, 0))
    .focusColor_(Color.hsv(0, 0, 0, 0));

    label = StaticText(view).string_(param.asString.capitalize).stringColor_(fgcolor.copy.alpha_(0.4));

    this.makeBounds;
  }

  makeBounds {
    this.bounds = Rect(clip.start * timelineView.scale, top, clip.dur * timelineView.scale * timelineView.timeline.tempo, height);
    label.bounds = Rect(15, view.bounds.height / 2 - 5, view.bounds.width, 10)
  }

  refreshEnv {
    view.setEnv(clip.perform(param));
  }

  top_ { |newtop|
    top = newtop;
    this.makeBounds;
  }

  height_ { |newheight|
    height = newheight;
    this.makeBounds;
  }

  color_ { |newcolor|
    color = newcolor;
    view.background_(color);
    this.changed(\color);
  }

  fgcolor_ { |newcolor|
    fgcolor = newcolor;
    view.strokeColor_(fgcolor).fillColor_(fgcolor);
    label.stringColor_(fgcolor.copy.alpha_(0.4));
    this.changed(\fgcolor);
  }
}




EnvelopeClipEditPanel : SCViewHolder {
  var parent, clip, bounds;
  var start, dur;
  var env, envRangePanel;
  var labelfont, monofont;
  var updatevals;

  *new { |parent, bounds, clip|
    ^super.new.init(parent, bounds, clip);
  }

  init { |argparent, argbounds, argclip|
    parent = argparent;
    bounds = argbounds;
    clip = argclip;

    labelfont = Font().size_(14).italic_(true);
    monofont = Font.monospace.size_(12);

    this.view = View(parent, bounds).resize_(5);

    updatevals = {
      start.string_(clip.start.asFloat.asStringPrec(log10(clip.start).floor + 3));
      dur.string_(clip.dur.asFloat.asStringPrec(log10(clip.dur).floor + 3));
    };

    clip.addDependant(updatevals);
    view.onClose_({ clip.removeDependant(updatevals); });

    StaticText(view, Rect(10, 60, bounds.width - 20, 20))
    .string_("Envelope")
    .font_(Font().size_(14).italic_(true))
    .resize_(2);

    StaticText(view, Rect(10, 310, (bounds.width - 30) / 2, 20))
    .string_("Start (beats)")
    .font_(labelfont)
    .resize_(2);

    start = TextField(view, Rect(10, 330, (bounds.width - 30) / 2, 20))
    .font_(monofont)
    .string_(clip.start)
    .action_({ |view|
      clip.start_(view.string.asFloat);
    });

    StaticText(view, Rect(bounds.width/2 + 5, 310, (bounds.width - 30) / 2, 20))
    .string_("Duration (sec)")
    .font_(labelfont)
    .resize_(2);

    dur = TextField(view, Rect(bounds.width/2 + 5, 330, (bounds.width - 30) / 2, 20))
    .font_(monofont)
    .string_(clip.dur)
    .action_({ |view|
      clip.dur_(view.string.asFloat);
    });


    env = TextView(view, Rect(10, 80, bounds.width - 20, 185))
    .font_(monofont)
    .string_(clip.env.asCompileString)
    .action_({ |view|
      clip.env_((view.string).interpret);
    });




    envRangePanel = View(view, Rect(0, 270, bounds.width, 30))
    .background_(Color.gray(0.8));

    StaticText(envRangePanel, Rect(10, 5, 90, 20))
    .string_("Range from")
    .font_(labelfont)
    .stringColor_(Color.gray);

    TextField(envRangePanel, Rect(100, 5, 80, 20))
    .font_(monofont)
    .string_(clip.rangeLo.asStringPrec(3))
    .action_({ |view|

    });

    StaticText(envRangePanel, Rect(185, 5, 50, 20))
    .string_("to")
    .font_(labelfont)
    .stringColor_(Color.gray);

    TextField(envRangePanel, Rect(210, 5, 80, 20))
    .font_(monofont)
    .string_(clip.rangeHi.asStringPrec(3))
    .action_({ |view|

    });




    Button(view, Rect(10, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Revert", Color.gray]])
    .acceptsMouse_(false) // button doesn't do anything yet
    .resize_(8);

    Button(view, Rect(bounds.width/2 + 5, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Commit", Color.gray]])
    .acceptsMouse_(false) // button doesn't do anything yet
    .resize_(8);
  }
}