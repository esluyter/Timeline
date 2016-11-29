PatternClipView : ClipView {

  init { |parent, argclip, argscale, argheight, argtop, argcolor, argfgcolor|
    timelineView = parent;
    clip = argclip;
    //scale = argscale;
    height = argheight;
    top = argtop;
    color = argcolor;
    fgcolor = argfgcolor;

    this.view_(UserView(parent)
      .background_(color)
      .drawFunc_({ |view|
        var runningtime = 0.0;
        var endtime = clip.dur + clip.startbeat;
        var stream = clip.pattern.source.asStream;
        var event = stream.next(clip.protoEvent);
        var xoffset = clip.startbeat * timelineView.scale;

        Pen.use {
          while { event.notNil } {
            var data = (event.use { (freq: event.freq, amp: event.amp, sustain: event.sustain, dur: event.dur) });

            var xscale = timelineView.scale;
            var startx = runningtime * xscale - xoffset;
            var width = data.sustain * xscale;

            var midinote = data.freq.cpsmidi;
            var yscale = view.bounds.height / 128;
            var y = view.bounds.height - (midinote * yscale);

            var thiscolor = fgcolor.copy.alpha_(data.amp.ampdb.linlin(-60.0, 0.0, 1.0, 0.0));

            Pen.color_(thiscolor);
            //Pen.moveTo(startx@y);
            //Pen.lineTo((startx+width)@y);
            //Pen.stroke;
            Pen.addRect(Rect(startx, y, width, max(this.height / 100, 2)));
            if (startx < 0) {
              //Pen.color_(Color.gray(0.8));
              Pen.color_(fgcolor.copy.alpha_(0.2));
              Pen.stroke;
            } {
              Pen.fill;
            };

            runningtime = runningtime + data.dur;
            if (runningtime < endtime) {
              event = stream.next(clip.protoEvent);
            } {
              event = nil;
            };
          };

        };
      });
    );



    this.visualinitstuff.draginitstuff;
    this.makeBounds;

  }

  makeContextMenu { |x, y|
    ContextMenu.create(this, x, y,
      "Edit pattern", { this.edit },
      "Rename clip", { this.rename },
      "Remove from Timeline", { this.remove },
      "", {},
      "Insert pattern code in current Document", { Document.current.selectedString_(clip.pattern.source.asCompileString) },
    );
  }

  makeEditPanel { |parent, bounds|
    var v = View(parent, bounds).resize_(5);

    StaticText(v, Rect(10, 10, bounds.width - 20, 40))
    .string_("Editing ‘" ++ clip.name ++ "’")
    .font_(Font().size_(30))
    .resize_(2);

    StaticText(v, Rect(10, 60, bounds.width - 20, 20))
    .string_("Pattern:")
    .font_(Font().italic_(true).size_(14))
    .resize_(2);

    TextView(v, Rect(10, 80, bounds.width - 20, bounds.height - 150))
    .string_(clip.pattern.asCompileString)
    .resize_(5);

    Button(v, Rect(10, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Revert"]])
    .resize_(8);

    Button(v, Rect(bounds.width/2 + 5, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Commit"]])
    .resize_(8);

    ^v;
  }

  copy {
    ^PatternClipView(this.parent, clip, nil, height, top + height + 10);
  }
}