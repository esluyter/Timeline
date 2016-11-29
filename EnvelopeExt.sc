+ Env {
  chop { |offset|
    var env = this.copy;
    var initlevel = env[offset];

    if (offset < 0) {
      env = env.delay(offset * -1);
    };
    if (offset > 0) {
      var runningtime = 0.0, i = 0;
      offset = offset * -1;
      while { (runningtime > offset) && (i < env.times.size) } {
        runningtime = runningtime - env.times[i];
        i = i + 1;
      };
      if (runningtime <= offset) { // passed target
        var timetokeep = offset - runningtime;
        env.levels = env.levels[(i - 1)..env.levels.size];
        env.times = env.times[(i - 1)..env.times.size];
        env.curves = if (env.curves.isArray) { env.curves[(i - 1)..env.curves.size] } { env.curves };
        env.times[0] = timetokeep;
        env.levels[0] = initlevel;
      } { // ran out of envelope
        env.times = [0];
        env.levels = initlevel ! 2;
        env.curves = \lin;
      };
    };

    ^env;
  }
}