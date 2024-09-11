To use JMeter, install it following the instructions here: https://jmeter.apache.org/usermanual/get-started.html

* Run Jetty using the test dataset: mvn jetty:run -DerddapContentDirectory=development/test
* For the main load tests, you most likely want to run those through the command line (the GUI uses a lot of memory)
* Add jmeter's bin directory to your system path. Then from the project root directory you can run the following:

```
jmeter -n -t .\development\jmeter\JMeterLoadTest.jmx -l .\development\jmeter\results.csv
```

If you want to use the GUI:

* Run JMeter.
* Load the JMeterLoadTest.
* Click the green arrow to run the tests.

## Profiling

If you want to profile ERDDAP™ VisualVM is a good option: https://visualvm.github.io/
You can attach it to the running Jetty server and collect profiling data.
Note that the "Profiler" for CPU monitoring will influence the results. More accurate CPU information (for actual cpu processing time) is available using "Sampler". In particular the "Profiler" will report time spent in small functions that are used heavily is way higher than it actually is.

To avoid the profiler overheard I've also used Mission Control https://github.com/openjdk/jmc. You can get binaries from vendors on that page.