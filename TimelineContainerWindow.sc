TimelineContainerWindow : SCViewHolder {
  var <win, <timelineView, <editPanel, editPanelWidth = 400, <shadowPanel, dependantFunc, clipview;

  *new { |bounds, timeline, scale = 100, cliptopheights, clipcolors, alwaysOnTop = false|
    ^super.new.init(bounds, timeline, scale, cliptopheights, clipcolors, alwaysOnTop);
  }

  init { |bounds, timeline, scale, cliptopheights, clipcolors, alwaysOnTop|
    bounds = bounds ?? Rect(Window.screenBounds.width/2 - 600, Window.screenBounds.height/2 - 400, 1200, 800);
    win = Window.new(timeline.name, bounds)
      .alwaysOnTop_(alwaysOnTop)
      .acceptsMouseOver_(true)
      .front;

    this.view_(win.view);

    timelineView = TimelineView(this, bounds.copy.origin_(0@0), timeline, scale, cliptopheights, clipcolors);

    editPanel = View(win, Rect(bounds.width - editPanelWidth, 0, editPanelWidth, bounds.height)).resize_(6).visible_(false);

    shadowPanel = View(win, Rect(bounds.width - editPanelWidth - 1, 0, 1, bounds.height)).resize_(6).background_(Color.gray(0, 0.5)).visible_(false);
  }

  clearEditPanel {
    timelineView.editing = nil;
    clipview.removeDependant(dependantFunc);
  }

  setEditPanel { |argclipview|
    var stripe, closebutt, clipname;

    clipview.removeDependant(dependantFunc);
    clipview = argclipview;

    editPanel.removeAll;
    if (clipview.notNil) {


      dependantFunc = {
        defer {
          stripe.background_(clipview.color);
          closebutt.stringColor_(clipview.fgcolor.copy.alpha_(0.2));
          clipname.stringColor_(clipview.fgcolor);
        };
      };
      clipview.addDependant(dependantFunc);

      editPanel.visible = true;
      shadowPanel.visible = true;

      timelineView.bounds = win.bounds.copy.origin_(0@0).resizeBy(-1 * editPanelWidth, 0);

      clipview.makeEditPanel(editPanel, editPanel.bounds.copy.origin_(0@0)).resize_(5);

      stripe = View(editPanel, Rect(0, 0, editPanel.bounds.width, 50))
      .background_(clipview.color)
      .mouseDownAction_({ |view, x, y, mod, butt, clicks|
        if (clicks > 1) {
          clipview.rename;
        };
      })
      .resize_(2);

      closebutt = StaticText(editPanel, Rect(5, 5, 35, 35))
      .string_("x")
      .font_(Font.sansSerif(30))
      .resize_(3)
      .stringColor_(clipview.fgcolor.copy.alpha_(0.2))
      .mouseEnterAction_({ |v|
        v.stringColor_(clipview.fgcolor.copy.alpha_(0.6))
      })
      .mouseLeaveAction_({ |v|
        v.stringColor_(clipview.fgcolor.copy.alpha_(0.2))
      })
      .mouseUpAction_({ |v|
        this.clearEditPanel;
      });

      clipname = StaticText(editPanel, Rect(40, 5, editPanel.bounds.width - 50, 40))
      .font_(Font().size_(20))
      .stringColor_(clipview.fgcolor)
      .mouseDownAction_({ |view, x, y, mod, butt, clicks|
        if (clicks > 1) {
          clipview.rename;
        };
      })
      .string_(clipview.clip.name);

    } {
      editPanel.visible = false;
      shadowPanel.visible = false;

      timelineView.bounds = win.bounds.copy.origin_(0@0);
    };
  }

}