TimelineClip : ClipTemplate {
  var <>startfunc, <>endfunc, server;

  *guiClass { ^TimelineClipView }

  *new { |start = 0, dur = 1, startfunc, endfunc, name="Code"|
    ^super.new.init(start, dur, startfunc, endfunc, name);
  }

  init { |argstart, argdur, argstartfunc, argendfunc, argname|
    start = argstart;
    dur = argdur;
    startfunc = argstartfunc;
    endfunc = argendfunc;
    name = argname;
  }

  play { |tempoClock, argServer|
    server = argServer ?? Server.default;
    tempoClock = tempoClock ?? TempoClock.default;

    server.makeBundle(server.latency, startfunc);

    stoprout = {
      dur.wait;
      server.makeBundle(server.latency, endfunc);
      isplaying = false;
    }.fork(tempoClock);

    isplaying = true;
  }

  stop {
    waitrout.stop;
    if (isplaying) {
      server.makeBundle(server.latency, endfunc);
      stoprout.stop;
    };
    isplaying = false;
  }

  storeArgs { // for compile string
    ^[start, dur, startfunc, endfunc, name]
  }
}
