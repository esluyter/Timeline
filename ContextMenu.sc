ContextMenu {
  classvar <>font;
  classvar rout;
  classvar menu;
  classvar thisview;

  *initClass {
    Class.initClassTree(Font);
    font = Font.default;
  }

  *remove { thisview.remove }

  *create { |clipview, x, y ...pairs|
    var items = [], functions = [];
    var height, width;
    var fontheight = " ".bounds(font).height * 1.1;
    var maxitemwidth = 10;
    var view = clipview.view;
    var parentview = view.parent;

    menu.remove; // only have one open at a time;

    pairs.pairsDo { |item, func|
      var testwidth;
      items = items.add(item);
      functions = functions.add(func);

      testwidth = item.asString.bounds(font).width * 1.2;
      if (testwidth > maxitemwidth) {
        maxitemwidth = testwidth;
      };
    };
    height = items.size * (fontheight) + 5;
    width = maxitemwidth;

    x = x + view.bounds.left - 1;
    y = y + view.bounds.top - 1;

    if ((x + width) > (parentview.visibleOrigin.x + parentview.bounds.width)) {
      x = max(parentview.visibleOrigin.x, parentview.visibleOrigin.x + parentview.bounds.width - width);
    };

    if ((y + height + 200) > (parentview.visibleOrigin.y + parentview.bounds.height)) {
      y = max(parentview.visibleOrigin.y, parentview.visibleOrigin.y + parentview.bounds.height - height - 200);
    };

    thisview = UserView(parentview, Rect(x, y, width, height + 200))
    .background_(Color.white)
    .mouseLeaveAction_({ |view| rout = fork { 0.6.wait; defer { view.remove; } } })
    .mouseOverAction_({ rout.stop })
    .focusLostAction_({ |view| view.remove; });

    menu = ListView(thisview, Rect(0, 0, width, height))
    .font_(font)
    .items_(items)
    .mouseOverAction_({|menu, x, y|
      var idx = (y/(fontheight)).floor;
      if (items[idx].asSymbol == '') {
        menu.selection_(-1);
      } {
        menu.selection_(idx);
      };

      rout.stop;
    })
    .mouseUpAction_({ |menu, x, y|
      functions.do { |func, i|
        if (menu.selection[0] == i) { func.value(view) };
      };
      thisview.remove;
    });


    StaticText(thisview, Rect(5, height, width - 10, 20)).string_("Clip color").stringColor_(Color.gray(0.5));
    5.do { |j|
      8.do { |i|
        var fgcolor = case
        { j == 4 } { Color.white }
        { (j == 3) && ((i == 0) || (i >= 5)) } { Color.white }
        { (j == 0) && (i <= 4) } { Color.white }
        { true } { Color.black };

        var bgcolor = case
        { j == 0 } { Color.gray(i.linlin(0, 7, 0, 1)) }
        { true } { Color.hsv(
          i.linlin(0, 8, 0, 1),
          case
          { j == 1 } { 0.1 }
          { j == 2 } { 0.3 }
          { j == 3 } { 1 }
          { j == 4 } { 1 },
          case
          { j == 1 } { 0.97 }
          { j == 2 } { 0.8 }
          { j == 3 } { 1 }
          { j == 4 } { 0.5 }
        )};

        if (clipview.color == bgcolor) {
          UserView(menu, Rect((((width - 5) / 8) * i), height + 15 + (25 * j), ((width - 5) / 8) + 5, 30))
          .background_(Color.gray);
        };

        Button(thisview, Rect(5 + (((width - 5) / 8) * i), height + 20 + (25 * j), ((width - 5) / 8) - 5, 20))
        .states_([["A", fgcolor, bgcolor]])
        .action_({ clipview.color = bgcolor; clipview.fgcolor = fgcolor; });
      };
    };
    menu.focus;
  }
}