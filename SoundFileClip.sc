SoundFileClip : ClipTemplate {
  classvar runningindex = 0;
  var index;

  var <server, <group, <synth;
  var <soundfile, <buffer, <filepath, <startframe;
  var <channels; // single integer or Array or nil for all channels
  var <outbus, <sendbus; // single integer or Array corresponding to channels array
  var <prefade, <panstereo; // boolean
  var <rate, <rateFunc, <senddb, <pan, <panFunc, <db; // single float, Env, or control Bus
  var <clipgain; // single float, in db
  var <clipenv; // Env for clip levels throughout clip
  // (basically masterdb = db + clipgain + clipenv)

  *guiClass { ^SoundFileClipView }

  *initClass {
    StartUp.add {
      SynthDescLib.new(\SoundFileClip, []);
    }
  }

  *new { |start, dur,
    filepath, startframe = 0, rate = 1.0,
    channels = nil, outbus = 0, db = 0.0,
    sendbus = nil, prefade = false, senddb = 0.0,
    clipgain = 0.0, clipenv, panstereo = true, pan = 0.0,
    name, server, group|
    ^super.new.init(start, dur,
      filepath, startframe, rate,
      channels, outbus, db,
      sendbus, prefade, senddb,
      clipgain, clipenv, panstereo, pan,
      name, server, group)
  }

  init { |argstart, argdur,
    argfilepath, argstartframe, argrate,
    argchannels, argoutbus, argdb,
    argsendbus, argprefade, argsenddb,
    argclipgain, argclipenv, argpanstereo, argpan,
    argname, argserver, arggroup|

    server = argserver ?? if (arggroup.notNil) { arggroup.server } ?? Server.default;
    group = arggroup ?? server.defaultGroup;

    this.server_(server); // register with synthdesclib

    start = argstart;
    startframe = argstartframe;

    outbus = argoutbus;
    db = argdb;
    sendbus = argsendbus;
    prefade = argprefade;
    senddb = argsenddb;
    clipgain = argclipgain;
    panstereo = argpanstereo;

    rate = if (argrate.isFunction) { argrate.value } { argrate };
    rateFunc = if (argrate.isFunction) { argrate } { ("{" ++ argrate.asCompileString ++ "}").interpret };

    pan = if (argpan.isFunction) { argpan.value } { argpan };
    panFunc = if (argpan.isFunction) { argpan } { ("{" ++ argpan.asCompileString ++ "}").interpret };

    index = runningindex;
    runningindex = runningindex + 1;

    this.make_filepath(argfilepath);
    this.make_dur(argdur);
    this.make_clipenv(argclipenv);
    this.make_name(argname);
    this.make_channels(argchannels);

    this.make_def;
  }

  durBeats { |tempo|
    ^(dur * tempo);
  }

  durBeats_ { |tempo, dur|
    this.dur = (dur / tempo);
  }

  synthdefname {
    ^("SoundFileClip" ++ index);
  }

  make_def {
    var thispanstereo = panstereo;

    if (((channels == 1) || (channels.size == 1)).not) {
      thispanstereo = false;
    };

    SynthDef(this.synthdefname, { |gate = 1, bufnum, startframe, /*rate,*/ db, prefade, /*senddb,*/ clipgain/*, pan*/|
      var thisrate = case
      { rate.class == Bus } { In.kr(rate) }
      { rate.class == Env } { EnvGen.ar(\rate.kr(rate.delay(0)), gate) }
      { true } { \rate.kr(rate) };
      var thissenddb = case
      { senddb.class == Bus } { In.kr(senddb) }
      { senddb.class == Env } { EnvGen.ar(\senddb.kr(senddb.delay(0)), gate) }
      { true } { senddb };
      var thispan = case
      { pan.class == Bus } { In.kr(pan) }
      { pan.class == Env } { EnvGen.ar(\pan.kr(pan.delay(0)), gate) }
      { true } { \pan.kr(pan) };

      var thisclipenv = \clipenv.kr(clipenv.delay(0));

      var sig = PlayBuf.ar(soundfile.numChannels, bufnum, BufRateScale.ir(bufnum) * thisrate, startPos: startframe);
      var outmul = (db + clipgain).dbamp;
      var outenv = EnvGen.kr(thisclipenv, 1, doneAction: 2) * outmul;
      var sendmul = Select.kr(prefade, [(db + clipgain + thissenddb).dbamp, (clipgain + thissenddb).dbamp]);
      var sendenv = EnvGen.kr(thisclipenv, 1) * sendmul;
      var thischannels = channels;

      var outsig, sendsig;

      var env = EnvGen.kr(Env.adsr(0.001, 0, 1, 0.001), gate, doneAction: 2);

      if (thispanstereo) {
        var idx = 0;
        if (channels.isArray) {
          idx = channels[0];
        };
        sig = Pan2.ar(sig[idx], thispan);
        thischannels = 2;
      };

      outsig = sig * outenv * env;
      sendsig = sig * sendenv * env;

      thischannels.do { |channel, i|
        var outtemp = outsig[channel];
        var sendtemp = sendsig[channel];
        var bustemp;

        if (outbus.isInteger) {
          bustemp = outbus + i
        } {
          bustemp = outbus[i];
        };
        Out.ar(bustemp, outsig[channel]);

        if (sendbus.notNil) {
          if (sendbus.isInteger) {
            bustemp = sendbus + i;
          } {
            bustemp = sendbus[i];
          };
          Out.ar(bustemp, sendsig[channel]);
        };

      };
    }).add(\SoundFileClip);
  }

  play { |offset = 0|
    var thisrate = if (rate.class == Env) { rate.chop(offset) } { rate };
    var thisclipenv = clipenv.chop(offset);
    var thispan = if (pan.class == Env) { pan.chop(offset) } { pan };
    var thissenddb = if (senddb.class == Env) { senddb.chop(offset) };

    var thisstartframe = startframe + if (rate.class == Env) {
      var ratesig = rate.asSignal(rate.duration * 100);
      if (offset > rate.duration) {
        ratesig = ratesig ++ Signal.series((offset - rate.duration) * 100, ratesig[ratesig.size - 1], 0);
      };
      (ratesig[0..((offset*100).floor.asInt)].integral / 100) * soundfile.sampleRate; // if an envelope, integrate it
    } {
      if (rate.class == Bus) {
        offset * soundfile.sampleRate; // no idea; just start from where you would start at rate = 1
      } {
        offset * soundfile.sampleRate * rate; // if a simple number, multiply it by sample rate
      };
    };

    server.makeBundle(server.latency, {
      synth.free;
      synth = Synth(this.synthdefname, [
        bufnum: buffer,
        startframe: thisstartframe,
        rate: thisrate,
        db: if (db.class == Bus) { db.asMap } { db },
        prefade: if (prefade) { 1 } { 0 },
        senddb: thissenddb,
        clipgain: clipgain,
        pan: thispan
      ], group);
    });

    stoprout = {
      dur.wait;
      server.makeBundle(server.latency, { synth.release });
      synth = nil;
      isplaying = false;
    }.fork(SystemClock); // time always in seconds

    isplaying = true;
  }

  stop {
    server.makeBundle(server.latency, { synth.release });
    synth = nil;
    isplaying = false;
    stoprout.stop;
    waitrout.stop;
  }

  schedule { |tempoClock, server, beatOffset = 0|
    var latency, offset, waittime;
    server = server ?? Server.default;
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

  free {
    synth.free;
    buffer.free;
  }

  // ---- these are used for init

  make_filepath { |argfilepath|
    filepath = argfilepath;
    soundfile = SoundFile();
    soundfile.openRead(filepath);
    buffer.free;
    buffer = Buffer.read(server, filepath);

    if ((channels.isArray).not) { channels = soundfile.numChannels };
  }

  filepath_ { |argfilepath, resetBounds = true, resetName = true|
    this.make_filepath(argfilepath);
    if (resetBounds) {
      this.startframe_(0);
      this.dur_(nil);
    };
    if (resetName) {
      this.name_(nil);
    };
    this.make_def;
    this.changed(\filepath, filepath);
    ^this;
  }

  make_channels { |argchannels|
    channels = argchannels;
    if (channels.notNil) {
      channels = channels.asArray;
    } {
      channels = soundfile.numChannels;
    };
  }

  channels_ { |argchannels|
    this.make_channels(argchannels);
    this.make_def;
    this.changed(\channels, channels);
    ^this;
  }

  make_dur { |argdur|
    dur = argdur ?? soundfile.duration; // in seconds!!! not beats
  }

  dur_ { |argdur|
    this.make_dur(argdur);
    this.changed(\dur, dur);
    ^this;
  }

  make_clipenv { |argclipenv|
    clipenv = argclipenv ?? Env([1, 1], [dur]);
  }

  clipenv_ { |argclipenv|
    this.make_clipenv(argclipenv);
    this.changed(\clipenv, clipenv);
    ^this;
  }

  make_name { |argname|
    name = argname ?? { PathName(filepath).fileName };
  }

  name_ { |argname|
    this.make_name(argname);
    this.changed(\name, name);
    ^this;
  }

  make_server { |newserver|
    server = newserver;
    SynthDescLib.getLib(\SoundFileClip).addServer(server).send(newserver);
  }

  server_ { |newserver|
    var changed = false;
    if (newserver != server) { changed = true };

    this.make_server(newserver);
    if (changed) { this.changed(\server, server) };
    ^this;
  }

  group_ { |newgroup|
    group = newgroup;
    this.server_(group.server);
    this.changed(\group, group);
    ^this;
  }

  // ---- other setters

  startframe_ { |argstartframe|
    startframe = argstartframe;
    this.changed(\startframe, startframe);
    this.changed(\startbeat);
    ^this;
  }

  rate_ { |argrate|
    rate = if (argrate.isFunction) { argrate.value } { argrate };
    rateFunc = if (argrate.isFunction) { argrate } { ("{" ++ argrate.asCompileString ++ "}").interpret };

    if (synth.notNil) {
      synth.set(\rate, rate);
    };

    this.make_def;
    this.changed(\rate, rate);
    ^this;
  }

  pan_ { |argpan|
    pan = if (argpan.isFunction) { argpan.value } { argpan };
    panFunc = if (argpan.isFunction) { argpan } { ("{" ++ argpan.asCompileString ++ "}").interpret };

    if (synth.notNil) {
      synth.set(\pan, pan);
    };

    this.make_def;
    this.changed(\pan, pan);
    ^this;
  }

  outbus_ { |argoutbus|
    outbus = argoutbus;
    this.make_def;
    this.changed(\outbus, outbus);
    ^this;
  }

  db_ { |argdb|
    db = argdb;
    this.make_def;
    this.changed(\db, db);
    ^this;
  }

  sendbus_ { |argsendbus|
    sendbus = argsendbus;
    this.make_def;
    this.changed(\sendbus, sendbus);
    ^this;
  }

  prefade_ { |argprefade|
    prefade = argprefade;
    this.changed(\prefade, prefade);
    ^this;
  }

  senddb_ { |argsenddb|
    senddb = argsenddb;
    this.make_def;
    this.changed(\senddb, senddb);
    ^this;
  }

  clipgain_ { |argclipgain|
    clipgain = argclipgain;
    this.changed(\clipgain, clipgain);
    ^this;
  }

  panstereo_ { |argpanstereo|
    panstereo = argpanstereo;
    this.make_def;
    this.changed(\panstereo, panstereo);
    ^this;
  }

  // Informational / add-on
  sampleRate {
    ^soundfile.sampleRate;
  }

  starttime {
    ^(startframe / this.sampleRate);
  }

  starttime_ { |newstarttime|
    ^(this.startframe_(newstarttime * this.sampleRate));
  }

  startbeat { |tempo = 1|
    ^(this.starttime * tempo);
  }

  startbeat_ { |startbeat, tempo = 1|
    ^(this.starttime_(startbeat / tempo));
  }

  // for compile string
  storeArgs {
    var thisclipenv = if (clipenv == Env([1, 1], [dur])) { nil } { clipenv };
    var thischannels = if (channels.isInteger) { nil } { channels };
    var thisdur = if (dur == soundfile.duration) { nil } { dur };
    var thisname = if (name == PathName(filepath).fileName) { nil } { name };
    var thisserver = nil;
    var thisgroup = nil;

    ^[start, thisdur, filepath, startframe, rate, thischannels, outbus, db, sendbus, prefade, senddb, clipgain, thisclipenv, panstereo, pan, thisname, thisserver, thisgroup];
  }
}