SoundFileClipView : ClipView {

  init { |parent, argclip, argscale, argheight, argtop, argcolor, argfgcolor|
    timelineView = parent;
    clip = argclip;
    //scale = argscale;
    height = argheight;
    top = argtop;
    color = argcolor;
    fgcolor = argfgcolor;

    paramViews = ();

    this.view_(SoundFileView(parent)
      .background_(color)
      .peakColor_(fgcolor.copy.alpha_(0.3))
      .rmsColor_(fgcolor.copy.alpha_(0.1))
      .yZoom_(4)
      .gridColor_(Color.white)
      .gridOn_(false)
      .soundfile_(clip.soundfile)
      .read(0, clip.soundfile.numFrames)
      .refresh
    );




    this.prerefresh;
    this.visualinitstuff.draginitstuff;
    this.makeBounds;

  }

  refreshFile {
    this.view.soundfile_(clip.soundfile).read(0, clip.soundfile.numFrames).refresh;
  }

  refreshEnv {
    if (clip.rate.class == Env) {
      if (paramViews[\rate].isNil) {
        timelineView.clearRange(top + height + (paramHeight * paramViews.size), paramHeight);
        paramViews[\rate] = EnvelopeParamView(view.parent, this, \rate, top + height, paramHeight, color, fgcolor);
      } {
        paramViews[\rate].refreshEnv;
      };
    } {
      if (paramViews[\rate].notNil) {
        paramViews[\rate].remove;
        paramViews[\rate] = nil;
      };
    };

    if (clip.pan.class == Env) {
      if (paramViews[\pan].isNil) {
        timelineView.clearRange(top + height + (paramHeight * paramViews.size), paramHeight);
        paramViews[\pan] = EnvelopeParamView(view.parent, this, \pan, top + height, paramHeight, color, fgcolor);
      } {
        paramViews[\pan].refreshEnv;
      };
    } {
      if (paramViews[\pan].notNil) {
        paramViews[\pan].remove;
        paramViews[\pan] = nil;
      };
    };

    this.makeBounds;
  }

  prerefresh {
    //view.read(clip.startframe, clip.dur * clip.sampleRate);
    //view.zoomToFrac((clip.dur * clip.sampleRate) / clip.soundfile.numFrames);
    //view.scrollPos()
    view.setSelectionColor(0, color);
    view.setSelection(0, [clip.startframe, clip.dur * clip.sampleRate]);
    view.zoomSelection(0);
  }

  makeBounds {
    this.bounds = Rect(clip.start * timelineView.scale, top, clip.dur * timelineView.scale * timelineView.timeline.tempo, height);
    border.bounds = this.bounds.copy.origin_(0@0);

    paramViews.do { |paramView, i|
      paramView.top_(top + height + (paramHeight * i));
      paramView.height_(paramHeight);
    }
  }

  makeContextMenu { |x, y|
    ContextMenu.create(this, x, y,
      "Edit parameters", { this.edit },
      "Edit sound file", {"Not yet implemented".postln},
      "Rename clip", { this.rename },
      "Remove from timeline", { view.remove },
      "", {},
      "Insert buffer code in current document", { Document.current.selectedString_("Buffer.read(s, \"" ++ clip.soundfile.path ++ "\")") },

    );
  }

  makeEditPanel { |parent, bounds|
    ^SoundFileClipEditPanel(parent, bounds, clip);
  }

  fgcolor_ { |newcolor|
    fgcolor = newcolor;
    namegui.stringColor_(fgcolor);
    view.peakColor_(fgcolor.copy.alpha_(0.3));
    view.rmsColor_(fgcolor.copy.alpha_(0.1));
    if (paramViews.notNil) {
      paramViews.do(_.fgcolor_(newcolor));
    };
    this.changed(\fgcolor);
  }

  copy {
    ^SoundFileClipView(this.parent, clip, nil, height, top + height + 10);
  }
}














SoundFileClipEditPanel : SCViewHolder {
  var parent, clip, bounds;
  var filepath, start, dur;
  var rate, rateAut, rateAutPanel;
  var pan, panAut, panAutPanel;
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
      filepath.string_(clip.filepath);
      start.string_(clip.start.asFloat.asStringPrec(log10(clip.start).floor + 3));
      dur.string_(clip.dur.asFloat.asStringPrec(log10(clip.dur).floor + 3));

      if (clip.rate.class == Env) {
        rateAut.value = true;
        rateAutPanel.visible_(true);
        rate.visible_(false;)
      } {
        rateAut.value = false;
        rateAutPanel.visible_(false);
        rate.visible_(true);
        rate.string_(clip.rateFunc.def.sourceCode.reverse[1..].reverse[1..])
      };

      if (clip.pan.class == Env) {
        panAut.value = true;
        panAutPanel.visible_(true);
        pan.visible_(false;)
      } {
        panAut.value = false;
        panAutPanel.visible_(false);
        pan.visible_(true);
        pan.string_(clip.panFunc.def.sourceCode.reverse[1..].reverse[1..])
      };
    };

    clip.addDependant(updatevals);
    view.onClose_({ clip.removeDependant(updatevals); });

    StaticText(view, Rect(10, 60, bounds.width - 20, 20))
    .string_("Sound File")
    .font_(Font().size_(14).italic_(true))
    .resize_(2);

    filepath = StaticText(view, Rect(10, 80, bounds.width - 20, 50))
    .string_(clip.filepath)
    .font_(monofont)
    .align_(\topLeft)
    .resize_(2)
    .mouseDownAction_({ |view, x, y, mods, buttnum, clickcount|
      if (clickcount > 1) {
        FileDialog({ |path| clip.filepath_(path) }, stripResult: true);
      }
    });

    StaticText(view, Rect(10, 140, (bounds.width - 30) / 2, 20))
    .string_("Start (beats)")
    .font_(labelfont)
    .resize_(2);

    start = TextField(view, Rect(10, 160, (bounds.width - 30) / 2, 20))
    .font_(monofont)
    .string_(clip.start)
    .action_({ |view|
      clip.start_(view.string.asFloat);
    });

    StaticText(view, Rect(bounds.width/2 + 5, 140, (bounds.width - 30) / 2, 20))
    .string_("Duration (sec)")
    .font_(labelfont)
    .resize_(2);

    dur = TextField(view, Rect(bounds.width/2 + 5, 160, (bounds.width - 30) / 2, 20))
    .font_(monofont)
    .string_(clip.dur)
    .action_({ |view|
      clip.dur_(view.string.asFloat);
    });







    StaticText(view, Rect(10, 190, bounds.width - 20, 20))
    .string_("Rate (float or control bus)")
    .font_(labelfont)
    .resize_(2);

    StaticText(view, Rect(10, 190, bounds.width - 40, 20))
    .string_("automate")
    .align_(\right)
    .font_(labelfont)
    .resize_(2);

    rateAut = CheckBox(view, Rect(bounds.width - 25, 193, 15, 15))
    .value_((clip.rate.class == Env))
    .action_({ |view|
      if (view.value) {
        clip.rate_(Env([1, 1], [1]));
      } {
        clip.rate_(1);
      };
    });

    rateAutPanel = View(view, Rect(0, 210, bounds.width, 30))
    .background_(Color.gray(0.8))
    .visible_(clip.rate.class == Env);

    StaticText(rateAutPanel, Rect(10, 5, 90, 20))
    .string_("Range from")
    .font_(labelfont)
    .stringColor_(Color.gray);

    TextField(rateAutPanel, Rect(100, 5, 80, 20))
    .font_(monofont)
    .string_("0")
    .action_({ |view|

    });

    StaticText(rateAutPanel, Rect(185, 5, 50, 20))
    .string_("to")
    .font_(labelfont)
    .stringColor_(Color.gray);

    TextField(rateAutPanel, Rect(210, 5, 80, 20))
    .font_(monofont)
    .string_("1")
    .action_({ |view|

    });

    rate = TextField(view, Rect(10, 210, bounds.width - 20, 20))
    .font_(monofont)
    .string_(clip.rateFunc.def.sourceCode.reverse[1..].reverse[1..])
    .action_({ |view|
      clip.rate_(("{" ++ view.string ++ "}").interpret);
    })
    .visible_((clip.rate.class == Env).not);





    StaticText(view, Rect(10, 240, bounds.width - 20, 20))
    .string_("Pan (float or control bus)")
    .font_(labelfont)
    .resize_(2);

    StaticText(view, Rect(10, 240, bounds.width - 40, 20))
    .string_("automate")
    .align_(\right)
    .font_(labelfont)
    .resize_(2);

    panAut = CheckBox(view, Rect(bounds.width - 25, 243, 15, 15))
    .value_((clip.pan.class == Env))
    .action_({ |view|
      if (view.value) {
        clip.pan_(Env([1, 1], [1]));
      } {
        clip.pan_(0);
      };
    });
    panAutPanel = View(view, Rect(0, 260, bounds.width, 30))
    .background_(Color.gray(0.8))
    .visible_(clip.rate.class == Env);

    StaticText(panAutPanel, Rect(10, 5, 90, 20))
    .string_("Range from")
    .font_(labelfont)
    .stringColor_(Color.gray);

    TextField(panAutPanel, Rect(100, 5, 80, 20))
    .font_(monofont)
    .string_("-1")
    .action_({ |view|

    });

    StaticText(panAutPanel, Rect(185, 5, 50, 20))
    .string_("to")
    .font_(labelfont)
    .stringColor_(Color.gray);

    TextField(panAutPanel, Rect(210, 5, 80, 20))
    .font_(monofont)
    .string_("1")
    .action_({ |view|

    });

    pan = TextField(view, Rect(10, 260, bounds.width - 20, 20))
    .font_(monofont)
    .string_(clip.panFunc.def.sourceCode.reverse[1..].reverse[1..])
    .action_({ |view|
      clip.pan_(("{" ++ view.string ++ "}").interpret);
    })
    .visible_((clip.pan.class == Env).not);




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