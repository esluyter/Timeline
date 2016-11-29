ClipTemplate {
  var <start, <dur, <name, isplaying = false;
  var stoprout, waitrout;

  guiClass { ^this.class.guiClass }

  schedule { |tempoClock, server, beatOffset = 0|
    var latency, waittime;
    server = server ?? Server.default;
    tempoClock = tempoClock ?? TempoClock.default;
    latency = server.latency * tempoClock.tempo; // adjust latency for tempo

    //tempoClock.schedAbs(beatOffset + start - latency, { this.play(tempoClock, server); nil });
    waittime = beatOffset + start - latency - tempoClock.beats;

    waitrout = {
      waittime.wait;
      this.play(tempoClock, server);
    }.fork(tempoClock);
  }

  durBeats { |tempo|
    ^dur;
  }

  durBeats_ { |tempo, dur|
    this.dur = dur;
  }

  // for registering dependants
  start_ { |argstart|
    start = argstart;
    this.changed(\start, start);
    ^this;
  }

  startbeat {
    ^0;
  }

  startbeat_ { |argstartbeat|
    ^this;
  }

  dur_ { |argdur|
    dur = argdur;
    this.changed(\dur);
    ^this;
  }

  name_ { |argname|
    name = argname;
    this.changed(\name, name);
    ^this;
  }
}