ClipView : SCViewHolder {
  var <timelineView;
  var paramViews, paramHeight = 40;
  var dragHeight = false, dragWidth = false, dragSide;
  var <clip, namegui, border;
  var <height, <top, <color, <fgcolor;
  var <dragfrom, oldbounds, readyfordrag = true, dragconstrain = false, mousedownbypass = true, dragging = false;
  var changefunc;
  var <editing = false, <>selected = false, dragSelected = false;

  *new { |parent, clip, scale = 100, height = 50, top = 0, color, fgcolor|
    color = color ?? { Color.gray(0.57142857142857) };
    fgcolor = fgcolor ?? { Color.white };
    ^super.new.init(parent, clip, scale, height, top, color, fgcolor);
  }

  edit {
    timelineView.editing = this;
  }

  copy {} // should override this in subclasses

  makeContextMenu { |x, y| // You should override this in subclasses
    ContextMenu.create(this, x, y,
      "Rename clip", { this.rename },
      "Remove from Timeline", { view.remove }
    );
  }

  makeEditPanel { |parent, bounds|
    var v = View(parent, bounds).resize_(5);
    var startfunclabel, startfuncbox, endfunclabel, endfuncbox;

    startfunclabel = StaticText(v, Rect(10, 60, bounds.width - 20, 20))
    .string_("Not yet implemented.... hang tight")
    .font_(Font().italic_(true).size_(14))
    .resize_(2);

    Button(v, Rect(10, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Revert", Color.gray]])
    .acceptsMouse_(false) // button doesn't do anything yet
    .resize_(8);

    Button(v, Rect(bounds.width/2 + 5, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Commit", Color.gray]])
    .acceptsMouse_(false) // button doesn't do anything yet
    .resize_(8);

    ^v;
  }

  editing_ { |bool|
    editing = bool;
    if (editing) {
      view.background_(color.copy.alpha_(0.4));
    } {
      view.background_(color);
    };
  }

  name {
    ^clip.name;
  }

  name_ { |newname|
    clip.name = newname;
  }

  height_ { |newheight|
    height = newheight;
    this.makeBounds;
  }

  top_ { |newtop|
    top = newtop;
    this.makeBounds;
  }

  color_ { |newcolor|
    color = newcolor;
    view.background_(color);
    border.refresh;
    if (paramViews.notNil) {
      paramViews.do(_.color_(newcolor));
    };
    this.changed(\color);
  }

  fgcolor_ { |newcolor|
    fgcolor = newcolor;
    namegui.stringColor_(fgcolor);
    view.refresh;
    border.refresh;
    if (paramViews.notNil) {
      paramViews.do(_.fgcolor_(newcolor));
    };
    this.changed(\fgcolor);
  }

  rename {
    var win, namefield;
    var width = 400, height = 170;
    var headerfont = Font().size_(30);

    win = Window("Rename clip", Rect(Window.screenBounds.width/2 - (width/2), Window.screenBounds.height/2 - (height/2), width, height), false, false)
    .alwaysOnTop_(true)
    .background_(color)
    .front;

    StaticText(win, Rect(0, 10, width, 50)).string_("Rename clip")
    .align_(\center)
    .stringColor_(fgcolor)
    .font_(headerfont);

    namefield = TextField(win, Rect(10, 80, width - 20, 30)).string_(this.name)
    .keyDownAction_({ |view, char, mod, unicode, keycode, key|
      switch (key)
      {16777220} { // return pressed
        this.name_(namefield.string); win.close;
      }
      {16777216} { // escape pressed
        win.close;
      };
    });

    Button(win, Rect(10, 130, width/2 - 15, 30)).states_([["OK"]])
    .action_{ this.name_(namefield.string); win.close };

    Button(win, Rect(width/2 + 5, 130, width/2 - 15, 30)).states_([["Cancel"]])
    .action_{ win.close };
  }

  draginitstuff {
    this.view.beginDragAction_({ |view, x, y|
      dragging = true;
      //dragfrom = x@y;
      oldbounds = this.bounds;
      if (mousedownbypass) { // hack to get around cmd-drag bypassing .mouseDownAction
        dragconstrain = true;
        dragSelected = true;
      } {
        dragconstrain = false;
      };
      this;
    })
    .keyDownAction_({|v, char, mod, uni, keycode, key|
      timelineView.handlekey(v, char, mod, uni, keycode, key);
    })
    .mouseDownAction_({ |view, x, y, mod, buttnum, clickcount|
      if (buttnum == 1) {
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

          if (clickcount > 1) {
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
          //mousedownbypass = false; // hack to get around cmd-drag bypassing this method
          //view.beginDrag(x, y);
          dragfrom = x@y;
        };
      };

      true; // bypass c++ mouse interaction!
    })
    .mouseMoveAction_({ |view, x, y, mod|
      if (dragging.not /*&& editing.not*/ && (dist(x@y, dragfrom) > 3)) {
        mousedownbypass = false; // hack to get around cmd-drag bypassing this method
        view.beginDrag(x, y);
      };
    })
    .mouseUpAction_({ |view, x, y|
      this.releasedrag;
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

    ^this;
  }

  refreshEnv {}
  refreshFile {}

  visualinitstuff {
    // update when parent clip updates
    changefunc = { |clip, what, val|
      case
      {what == \name} { namegui.string_(val) }
      {what == \start} { this.prerefresh; this.makeBounds; }
      {what == \dur} { this.prerefresh; this.makeBounds; }
      {what == \rate} { this.refreshEnv; }
      {what == \pan} { this.refreshEnv; }
      {what == \startbeat} { this.prerefresh; this.makeBounds; }
      {what == \refresh} { this.prerefresh; this.makeBounds }
      {what == \filepath } { this.refreshFile }
      {what == \env} { this.refreshEnv }
    };

    clip.addDependant(changefunc);

    namegui = StaticText(view, Rect(3, 3, view.bounds.width, 15)).string_(clip.name).stringColor_(fgcolor);

    border = UserView(view, view.bounds.copy.origin_(0@0))
    .drawFunc_({
      Pen.strokeColor_(fgcolor);
      Pen.width_(if (editing) { 6 } { if (selected) { 3 } { 1 } });
      Pen.strokeRect(view.bounds.origin_(0@0));
      if (selected) {
        Pen.width_(2);
        Pen.strokeColor_(Color.cyan);
        Pen.strokeRect(view.bounds.origin_(0@0));
      };
    })
    .acceptsMouse_(false)
    .resize_(5);

    ^this;
  }

  prerefresh { }

  makeBounds {
    this.bounds = Rect(clip.start * timelineView.scale, top, clip.dur * timelineView.scale, height);
    border.bounds = this.bounds.copy.origin_(0@0);
  }

  releasedrag {
    mousedownbypass = true; // reset hack
    dragHeight = false;
    dragWidth = false;
    dragging = false;
    dragSelected = false;
  }

  setheight { |newheight, dragSide|
    var newtop = if (dragSide == \top) {
      this.bounds.height - newheight + this.bounds.top;
    } {
      this.bounds.top;
    };

    if (newheight > 10) {
      this.bounds = this.bounds.top_(newtop).height_(newheight);
      border.bounds = this.bounds.copy.origin_(0@0);
      height = newheight;
      top = newtop;
    };
  }

  dragheight { |diff|
    var newheight;

    if (dragSide == \top) {
      newheight = this.bounds.height - diff.y;
    } {
      newheight = this.bounds.height + diff.y;
      dragfrom.y = dragfrom.y + diff.y
    };

    this.setheight(newheight, dragSide);
  }

  dragwidth { |diff|
    if (dragSide == \left) { // resize from left
      var startdiff = diff.x / timelineView.scale;
      var newstartbeat = clip.startbeat(timelineView.timeline.tempo) + startdiff;
      var newstart = clip.start + startdiff;
      var newdur = clip.durBeats(timelineView.timeline.tempo) - startdiff;

      clip.startbeat_(newstartbeat, timelineView.timeline.tempo);
      clip.start_(newstart);
      clip.durBeats_(timelineView.timeline.tempo, newdur);
    } { // resize from right
      var newdur = clip.durBeats(timelineView.timeline.tempo) + (diff.x / timelineView.scale);
      clip.durBeats_(timelineView.timeline.tempo, newdur);
      dragfrom.x = dragfrom.x + diff.x;
    };
  }

  dragstart { |diff|
    var bounds = this.bounds.copy.top_(this.bounds.top + diff.y).left_(this.bounds.left + diff.x);
    var newstart = bounds.left / timelineView.scale;
    var newdur = clip.durBeats(timelineView.timeline.tempo);
    var newtop = bounds.top;
    var newheight = bounds.height;

    this.bounds = this.bounds.top_(newtop).height_(newheight);
    height = newheight;
    top = newtop;

    if (dragconstrain.not) {
      this.clip.start_(newstart).durBeats_(timelineView.timeline.tempo, newdur);
    };
  }

  dragbounds { |diff|
    if (readyfordrag) {
      if (dragHeight || dragWidth) {
        if (dragHeight) {
          this.dragheight(diff);
        } {
          this.dragwidth(diff);
        };
      } {
        this.dragstart(diff);
      };

      if (dragSelected && (dragHeight || dragWidth).not) {
        timelineView.clipviews.do { |clipview|
          if (clipview.selected && (clipview != this)) {
            clipview.dragbounds(if (dragconstrain) { 0@diff.y } { diff });
          };
        };
      };

      if (dragSelected && dragHeight) {
        timelineView.clipviews.do { |clipview|
          if (clipview.selected && (clipview != this)) {
            clipview.setheight(height, dragSide);
          };
        };
      };

      timelineView.refreshBarlines;
      timelineView.refreshPlayhead;

      readyfordrag = false;
      fork { 0.05.wait; readyfordrag = true; }
    }
  }

  remove {
    timelineView.timeline.remove(clip);
    view.remove;
  }
}