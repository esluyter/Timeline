EnvelopeClip : ClipTemplate {
  var <env, <outbus, <ar, synth, group, server, <rangeLo, <rangeHi;

  *guiClass { ^EnvelopeClipView }

  *initClass {
    StartUp.add {
      SynthDescLib.new(\EnvelopeClip, []);
    }
  }

  *new { |start = 0, env, outbus=0, ar=false, name="Envelope", server, group|
    ^super.new.init(start, env, outbus, ar, name, server, group);
  }

  init { |argstart, argenv, argoutbus, argar, argname, argserver, arggroup|
    start = argstart;
    env = argenv;
    outbus = argoutbus;
    ar = argar;
    name = argname;

    rangeLo = env.levels.minItem;
    rangeHi = env.levels.maxItem;

    rangeLo = rangeLo - (rangeHi - rangeLo * 0.2);
    rangeHi = rangeHi + (rangeHi - rangeLo * 0.2);

    server = argserver ?? if (arggroup.notNil) { arggroup.server } ?? Server.default;
    group = arggroup ?? server.defaultGroup;
    this.server_(server); // register with synthdesclib

    this.make_def;
  }

  play { |offset=0|
    var thisenv = env.chop(offset);
    server.makeBundle(server.latency, {
      synth.free;
      if (ar) {
        "AR envelopes not yet implemented!".postln;
      } {
        synth = Synth(\EnvelopeClipKr, [\out: outbus, \env: thisenv], group);
      };
    });

    stoprout = {
      (this.dur).wait;
      server.makeBundle(server.latency, { synth.free; synth = nil; });
      isplaying = false;
    }.fork(SystemClock);

    isplaying = true;
  }

  stop {
    waitrout.stop;
    stoprout.stop;
    if (isplaying) {
      server.makeBundle(server.latency, { synth.free; synth = nil; });
    };
    isplaying = false;
  }

  make_def {
    SynthDef(\EnvelopeClipKr, { |out|
      var sig = EnvGen.kr(\env.kr(env.delay(0)));
      Out.kr(out, sig);
    }).add(\EnvelopeClip);
  }

  env_ { |argenv|
    env = argenv;
    this.make_def;
    this.changed(\env, env);
    ^this;
  }

  dur {
    ^env.duration;
  }

  dur_ { |newdur|
    env.duration = newdur;
    this.changed(\dur, newdur);
    ^this;
  }

  durBeats { |tempo|
    ^(env.duration * tempo);
  }

  durBeats_ { |tempo, dur|
    this.dur = (dur / tempo);
  }

  outbus_ { |argoutbus|
    outbus = argoutbus;
    this.changed(\outbus, outbus);
    ^this;
  }

  ar_ { |argar|
    ar = argar.asBoolean;
    this.changed(\ar, ar);
    ^this;
  }

  make_server { |newserver|
    server = newserver;
    SynthDescLib.getLib(\EnvelopeClip).addServer(server).send(newserver);
  }

  server_ { |newserver|
    var changed = false;
    if (newserver != server) { changed = true };

    this.make_server(newserver);
    if (changed) { this.changed(\server, server) };
    ^this;
  }

  schedule { |tempoClock, argserver, beatOffset = 0|
    var latency, offset, waittime;
    tempoClock = tempoClock ?? TempoClock.default;
    latency = server.latency * tempoClock.tempo; // adjust latency for tempo

    offset = if (tempoClock.beats < (beatOffset + start - latency)) {
      0;
    } {
      ((tempoClock.beats - (beatOffset + start - latency)) / tempoClock.tempo);
    };

    //tempoClock.schedAbs(beatOffset + start - latency, { this.play(offset); nil });
    waittime = beatOffset + start - latency - tempoClock.beats;

    waitrout = {
      waittime.wait;
      this.play(offset);
    }.fork(tempoClock);
  }

  storeArgs { // for compile string
    ^[start, env, outbus, ar, name]
  }
}