TimelineClipView : ClipView {

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
        Pen.use {
          var drawstrings = clip.startfunc.def.sourceCode.split($\n);
          Pen.color_(fgcolor.copy.alpha_(0.4));
          drawstrings.do { |string, i|
            Pen.stringAtPoint(string, 0@(i * 10 + 14), Font("Monaco", 8));
          };
        };
      });
    );



    this.visualinitstuff.draginitstuff;
    this.makeBounds;

  }

  makeContextMenu { |x, y|
    ContextMenu.create(this, x, y,
      "Edit code", { this.edit },
      "Rename clip", { this.rename },
      "Remove from Timeline", { this.remove },
      "", {},
      "Insert startfunc code in current Document", { Document.current.selectedString_(clip.startfunc.def.sourceCode) },
      "Insert endfunc code in current Document", { Document.current.selectedString_(clip.endfunc.def.sourceCode) }
    );
  }

  makeEditPanel { |parent, bounds|
    var v = View(parent, bounds).resize_(5);
    var startfunclabel, startfuncbox, endfunclabel, endfuncbox;

    startfunclabel = StaticText(v, Rect(10, 60, bounds.width - 20, 20))
    .string_("Start Function:")
    .font_(Font().italic_(true).size_(14))
    .resize_(2);

    startfuncbox = TextView(v, Rect(10, 80, bounds.width - 20, (bounds.height - 180) / 2))
    .string_(clip.startfunc.def.sourceCode)
    .resize_(5);

    endfunclabel = StaticText(v, Rect(10, (bounds.height - 180) / 2 + 90, bounds.width - 20, 20))
    .string_("End Function:")
    .font_(Font().italic_(true).size_(14))
    .resize_(8);

    endfuncbox = TextView(v, Rect(10, (bounds.height - 180) / 2 + 110, bounds.width - 20, (bounds.height - 180) / 2))
    .string_(clip.endfunc.def.sourceCode)
    .resize_(8);

    Button(v, Rect(10, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Revert"]])
    .resize_(8);

    Button(v, Rect(bounds.width/2 + 5, bounds.height - 55, (bounds.width - 30) / 2, 40))
    .states_([["Commit"]])
    .resize_(8);

    ^v;
  }

  copy {
    ^TimelineClipView(this.parent, clip, nil, height, top + height + 10);
  }
}
