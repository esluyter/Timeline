PatternClip : ClipTemplate {
  var <>pattern, <>protoEvent, player;
  var <startbeat;

  *guiClass { ^PatternClipView }

  *new { |start = 0, dur = 1, pattern, startbeat, protoEvent, name|
    ^super.new.init(start, dur, pattern, startbeat, protoEvent, name);
  }

  init { |argstart, argdur, argpattern, argstartbeat, argprotoEvent, argname|
    pattern = argpattern;
    start = argstart;
    startbeat = argstartbeat;
    dur = argdur;
    protoEvent = argprotoEvent ?? Event.default;

    name = argname ?? {
      if (pattern.respondsTo(\key)) {
        name = pattern.key;
      } {
        name = pattern.source.asStream.next(Event.default).instrument;
      };
    };
  }

  play { |tempoClock, server|
    //var stream = Pfindur(dur, pattern).asStream;
    var stream = pattern.asStream;

    server = server ?? Server.default;
    tempoClock = tempoClock ?? TempoClock.default;

    //tempoClock.sched(0 - startbeat, {player = EventStreamPlayer(stream, protoEvent).play(tempoClock, quant: 0); nil});
    { // TODO find out why this creates a lot of zombie synths
      stream.fastForward(startbeat, 0, protoEvent).wait;
      player = EventStreamPlayer(stream, protoEvent).play(tempoClock, quant:0);
    }.fork(tempoClock);

    stoprout = {
      dur.wait;
      this.stop;
    }.fork(tempoClock);

    isplaying = true;
  }

  stop {
    waitrout.stop;
    if (isplaying) {
      player.stop;
      stoprout.stop;
    };
    isplaying = false;
  }

  storeArgs { // for compile string
    var event;

    if (protoEvent == Event.default) {
      event = nil;
    } {
      event = protoEvent;
    };
    ^[start, dur, pattern, event];
  }

  // for registering dependants
  startbeat_ { |argstartbeat|
    startbeat = argstartbeat;
    this.changed(\startbeat);
    ^this;
  }
}