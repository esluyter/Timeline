Timeline {
  var <name, <clips, <clock, <server, <tempo = 2, beats = 0;

  *new { |name = "Untitled", clips, tempo = 2|
    ^super.new.init(name, clips, tempo);
  }

  init { |argname, argclips, argtempo|
    name = argname;
    clips = argclips ?? [];
    tempo = argtempo;
    clock = TempoClock.new(tempo, 0);
    clock.clear;
  }

  add { |clip|
    clips = clips.add(clip);
  }

  remove { |clip|
    clips.removeAt(clips.indexOf(clip));
  }

  beats {
    ^(beats ?? clock.beats);
  }

  beats_ { |newbeats|
    if (beats.notNil) {
      beats = newbeats;
    } {
      this.stop;
      beats = newbeats;
      this.play(beats);
    };
  }

  isplaying {
    ^(beats.isNil);
  }

  play { |beatOffset, tempoClock, argServer, playPartialEvents = false|
    beatOffset = beatOffset ?? beats;
    beats = beatOffset;

    server = argServer ?? Server.default;

    clock = tempoClock ?? TempoClock.new(tempo, beatOffset);
    clock.beats = beats;

    clips.do { |clip|
      if (clip.start >= beats) {
        clip.schedule(clock, server)
      } {
        if (((clip.start + clip.durBeats(tempo)) >= beats) && playPartialEvents) {
          clip.schedule(clock, server)
        };
      };
    };

    beats = nil;
  }

  stop {
    beats = clock.beats;
    fork {
      clips.do(_.stop);
      server.sync;
      clock.queue.do {|x| x.value };
      clock.clear;
    }
  }

  hardstop {
    clock.clear;
  }

  tempo_ { |newtempo|
    tempo = newtempo;
    clock.tempo = newtempo;
  }

  storeArgs { // for compile string
    ^[name, clips, tempo];
  }

  duration {
    var maxdur = 0;
    clips.do { |clip|
      var clipend = clip.start + clip.durBeats(tempo);
      if (clipend > maxdur) { maxdur = clipend };
    };
    ^maxdur;
  }
}