TimelineView : SCViewHolder {
  var <>centerPlayhead = true, <>cursorFollowsPlayback = false, <beatsPerBar = 4;
  var <timeline, <scale, <cursorpos = 0;
  var placeholder, playhead;
  var defaultParent = false, defaultBounds = false;
  var cursor, playhead, playheadrout;
  var <barlines;
  var <clipviews;
  var <editing, timelineContainerWindow;
  var selectionBox, selectionOrigin;

  *new { |parent, bounds, timeline, scale = 100, cliptopheights, clipcolors|
    // timeline = parent Timeline
    // scale = pixels/seconds
    // cliptopheights = optional array specifying vertical positioning
    ^super.new.init(parent, bounds, timeline, scale, cliptopheights, clipcolors);
  }

  init { |parent, bounds, argtimeline, argscale, cliptopheights, clipcolors|
    clipviews = [];

    if (parent.class.postln == TimelineContainerWindow) {
      timelineContainerWindow = parent;
    };

    // make window if no parent specified
    parent = parent ?? {
      defaultParent = true;
      Window.new(argtimeline.name, Rect(Window.screenBounds.width/2 - 400, Window.screenBounds.height/2 - 300, 800, 600))
      .alwaysOnTop_(true)
      .acceptsMouseOver_(true)
      .front
    };

    // bounds of parent if no bounds specified
    bounds = bounds ?? {
      defaultBounds = true;
      Rect(0, 0, parent.bounds.width, parent.bounds.height)
    };

    timeline = argtimeline;
    scale = argscale;
    cliptopheights = cliptopheights ?? [];
    clipcolors = clipcolors ?? [];

    // Main timeline view -- a ScrollView with default canvas
    this.view_(ScrollView(parent, bounds)
      .resize_(5)
      .action_({
        this.refreshBarlines;
      })
      .onResize_({
        this.refreshBarlines;
        this.refreshPlayhead;
      })
    );


    // create barlines background
    barlines = UserView(this, view.innerBounds)
    .drawFunc_({
      var maxtime = view.innerBounds.width / scale;
      var height = view.innerBounds.height;
      Pen.use {
        maxtime.floor.do { |i|
          var subbeat = i % beatsPerBar;
          var barnum = (i / beatsPerBar).floor;

          if (subbeat == 0) {
            var secsdecimal, timestring;
            var secs = 1.0 * i / timeline.clock.tempo;
            var min = (secs / 60).floor;
            var hour = (min / 60).floor;
            secs = secs - (min * 60);
            min = min - (hour * 60);

            secsdecimal = secs;
            secs = secs.floor;
            secsdecimal = secsdecimal - secs;

            timestring = " ";
            if (hour > 0) {
              timestring = timestring ++ hour.asString ++ ":";
            };
            timestring = timestring ++ min.asString.padLeft(2, "0") ++ ":";
            timestring = timestring ++ secs.asString.padLeft(2, "0");
            if (secsdecimal != 0.0) {
              timestring = timestring ++ "." ++ secsdecimal.asString[2..];
            };


            Pen.color_(Color.gray(0.5));
            Pen.stringAtPoint(" " ++ barnum, Point(i*scale, 0));
            Pen.stringAtPoint(timestring, Point(i*scale, height - 20));
          } {
            Pen.color_(Color.gray(0.5, 0.3));
          };
          Pen.addRect(Rect(i*scale, 0, 1, height));
          Pen.fill;
        }
      }
    })
    .keyDownAction_({ |v, char, mod, uni, keycode, key|
      this.handlekey(v, char, mod, uni, keycode, key);
    })
    .mouseDownAction_({ |v, x, y, mod, buttnum, clicks|
      var origin = view.visibleOrigin;
      var realcoords = x@y;// + origin;

      ContextMenu.remove;

      if (clicks > 1) {
        this.editing_(nil);
      };

      this.select(nil);

      this.cursorpos_(realcoords.x / scale);

      selectionBox = UserView(view, Rect(x, y, 1, 1)).background_(Color.gray(0.5, 0.2)).drawFunc_({ |v|
        Pen.color_(Color.white);
        Pen.width_(3);
        Pen.strokeRect(v.bounds.copy.origin_(0@0));
        Pen.color_(Color.black);
        Pen.width_(1);
        Pen.strokeRect(v.bounds.copy.origin_(0@0));
      });
      selectionOrigin = x@y;
    })
    .mouseMoveAction_({ |v, x, y, mod|
      var origin = view.visibleOrigin;
      var realcoords = x@y;// + origin;

      this.cursorpos_(realcoords.x / scale);

      selectionBox.bounds = Rect(min(selectionOrigin.x, x), min(selectionOrigin.y, y), abs(x - selectionOrigin.x), abs(y - selectionOrigin.y));

      this.selectRange(selectionBox.bounds);
    })
    .mouseUpAction_({
      selectionBox.remove;
    })
    .canReceiveDragHandler_({|v, x, y| // receives objects dragged to make new objects or drag existing ones
      var origin = view.visibleOrigin;
      var realcoords = x@y;// + origin;

      if (View.currentDrag.class.superclass == ClipView) {
        var clip = View.currentDrag;
        var diff = (realcoords - clip.dragfrom) - (clip.bounds.left@clip.bounds.top);
        //clip.dragbounds(clip.bounds.top_(realcoords.y - clip.dragfrom.y).left_(realcoords.x - clip.dragfrom.x));
        clip.dragbounds(diff);
      } {
        var str = View.currentDragString.interpret;
        if (File.exists(str)) { // make placeholder for sound files
          /*
          if (placeholder.isNil) {
          placeholder = PatternClipView(view, Rect(realcoords.x, realcoords.y + 1, 350, 49), nil, 5, str);
          } {
          placeholder.bounds = placeholder.bounds.top_(realcoords.y + 1).left_(realcoords.x);
          };
          */
        };
      };
      true;
    })
    .receiveDragHandler_({|v, x, y| // create new objects or move existing ones
      var origin = view.visibleOrigin;
      var realcoords = x@y; //+ origin;

      placeholder.remove;
      placeholder = nil;

      if ((View.currentDrag.class.superclass == ClipView)) {
        var clip = View.currentDrag;
        var diff = (realcoords - clip.dragfrom) - (clip.bounds.left@clip.bounds.top);
        //clip.dragbounds(clip.bounds.top_(realcoords.y - clip.dragfrom.y).left_(realcoords.x - clip.dragfrom.x));
        clip.dragbounds(diff);
        clip.releasedrag;
      } {
        var str = View.currentDragString.interpret;
        if (File.exists(str)) {
          //SoundFileClipView(v, Rect(realcoords.x, realcoords.y, 350, 50), View.currentDragString.interpret);
        } {
          var obj = str.interpret; // Figure out how to get a Synth without running it.....
          if (obj.isPattern) {
            var clip = PatternClip(x / scale, 5, obj);
            timeline.add(clip);
            PatternClipView(view, clip, scale, 50, y);
          };
        }
      };
    });

    // create views for all the timeline clips using provided information about vertical positioning
    timeline.clips.do { |clip, i|
      var newcolor, newfgcolor;
      var gap = 5;
      var newheight = 50;
      var testleft = clip.start * scale;
      var testright = (clip.start + clip.dur) * scale;
      var conflictingbounds = clipviews.collect(_.bounds)
      .reject({ |a| (a.left > testright) || ((a.left + a.width) < testleft) })
      .sort({ |a, b| a.top < b.top });

      var lowestconflict = conflictingbounds[conflictingbounds.size - 1];
      var highestconflict = conflictingbounds[0];
      var newtop = case
      { lowestconflict.notNil } {
        if (newheight + 25 + gap <= highestconflict.top) { 25 } { lowestconflict.top + lowestconflict.height + gap }
      }
      { lowestconflict.isNil } { 25 };

      if (cliptopheights[i].notNil) {
        newtop = cliptopheights[i][0];
        newheight = cliptopheights[i][1];
      };

      if (clipcolors[i].notNil) {
        newcolor = clipcolors[i][0];
        newfgcolor = clipcolors[i][1];
      };

      clipviews = clipviews.add(clip.guiClass.new(this, clip, scale, newheight, newtop, newcolor, newfgcolor));
    };

    //this.editing_(clipviews[0]);

    // create playhead and cursor
    playhead = UserView(this, Rect(timeline.beats * scale, 0, 1, this.bounds.height))
    .background_(/*Color.red(1, 0.4)*/Color.white)
    .acceptsMouse_(false);

    cursor = UserView(this, Rect(cursorpos * scale, 0, 1, this.bounds.height))
    .background_(Color.black)
    .acceptsMouse_(false);

  }

  handlekey { |v, char, mod, uni, keycode, key|
    if (key == 32) {
      if (timeline.isplaying) {
        timeline.stop;
        playheadrout.stop;
        if (cursorFollowsPlayback) {
          this.cursorpos_(timeline.beats);
        };
      } {
        timeline.play(cursorpos, playPartialEvents: true);
        playheadrout = { inf.do{ this.refreshPlayhead; 0.05.wait; } }.fork(AppClock);
      };
    };

    if (key == 16777220) {
      this.cursorpos_(0);
    };

    if (mod.isCmd) {
      var factor = if (mod.isShift) { 1.05 } { 1.5 };
      if (key == 61) { // Cmd +
        this.scale_(scale * factor);
      };
      if (key == 45) { // Cmd -
        this.scale_(scale / factor);
      };
      if (key == 48) { // Cmd 0
        this.scale_(view.bounds.width / timeline.duration);
        view.visibleOrigin_(Point(0, view.visibleOrigin.y));
      };
    }
  }

  lowestClipView {
    var y = 0;
    clipviews.do { |clipview|
      var lowpoint = clipview.top + clipview.height;
      if (lowpoint > y) { y = lowpoint };
    };
    ^y;
  }

  refreshPlayhead {
    playhead.bounds = Rect(timeline.beats * scale, 0, 1,  max(this.lowestClipView + 30, view.bounds.height));
    cursor.bounds = Rect(cursorpos * scale, 0, 1, max(this.lowestClipView + 30, view.bounds.height));
    if (timeline.isplaying && centerPlayhead) {
      view.visibleOrigin_(Point(
        playhead.bounds.left - (view.bounds.width / 2),
        view.visibleOrigin.y
      ));
    };
  }

  refreshBarlines {
    barlines.bounds_(Rect(0, 0, view.innerBounds.width, max(this.lowestClipView + 30, view.bounds.height)));
    barlines.refresh;
  }

  editing_ { |clipview|
    editing = clipview;
    clipviews.do(_.editing_(false));
    if (clipview.notNil) {
      clipview.editing = true;
    };
    clipviews.do(_.refresh);
    if (timelineContainerWindow.notNil) {
      timelineContainerWindow.setEditPanel(clipview);
    };
  }

  clearRange { |top, height|
    var clipsinrange = false;
    clipviews.do({ |clipview|
      if ((clipview.top >= top) && (clipview.top <= (top + height))) {
        //clipview.top_(clipview.top + height);
        clipsinrange = true;
      };
    });
    if (clipsinrange) {
      clipviews.do { |clipview|
        if (clipview.top >= top) {
          clipview.top_(clipview.top + height);
        };
      };
    };
  }

  select { |clipview|
    clipviews.do(_.selected_(false));
    if (clipview.notNil) {
      clipview.selected = true;
    };
    clipviews.do(_.refresh);
  }

  selectRange { |rect|
    clipviews.do { |clipview|
      clipview.selected = clipview.bounds.intersects(rect);
      clipview.refresh;
    };
  }

  addSelection { |clipview|
    clipview.selected = true;
    clipview.refresh;
  }

  removeSelection { |clipview|
    clipview.selected = false;
    clipview.refresh;
  }

  beatsPerBar_ { |val|
    beatsPerBar = val;
    this.refreshBarlines;
  }

  cursorpos_ { |newcursorpos|
    cursorpos = newcursorpos;
    timeline.beats = cursorpos;
    this.refreshPlayhead;
  }

  scale_ { |newscale|
    var visibleOrigin = view.visibleOrigin;
    var oldscale = scale;

    scale = newscale;
    this.refresh;
    this.refreshPlayhead;
    this.cursorpos_(cursorpos);
    clipviews.do(_.makeBounds);
    //if (centerPlayhead) {
    view.visibleOrigin_(Point(
      cursor.bounds.left - (view.bounds.width / 2),
      view.visibleOrigin.y
    ));
    //};
  }

  storeArgs { // for compile string
    var cliptopheights = clipviews.collect { |clipview|
      [clipview.bounds.top, clipview.bounds.height]
    };
    var parent = if (defaultParent) {nil} {view.parent};
    var bounds = if (defaultBounds) {nil} {view.bounds};
    ^[parent, bounds, timeline, scale, cliptopheights];
  }
}

