# Benchmark results

## 92306116f5bc

```bash
sbt 'benchmarks/jmh:run -i 5 -wi 3 -f1 -t1 org.lichess.compression.benchmark.*'
```

scala 3.7.4 / java 25

```
[info] Benchmark                       Mode  Cnt     Score    Error  Units
[info] BitOpsTest.testRead             avgt    5   110.431 ±  1.492  ns/op
[info] HuffmanPgnBench.decode          avgt    5  2941.881 ± 12.174  us/op
[info] HuffmanPgnBench.encode          avgt    5  2715.461 ± 16.529  us/op
[info] LinearEstimateTest.testEncode   avgt    5    32.990 ±  0.419  ns/op
[info] LowBitTruncTest.testEncode      avgt    5    17.890 ±  0.045  ns/op
[info] OverallEncodingTest.testDecode  avgt    5   133.375 ±  8.684  ns/op
[info] OverallEncodingTest.testEncode  avgt    5   172.780 ±  2.920  ns/op
[info] VarIntEncodingTest.testDecode   avgt    5    84.011 ±  0.825  ns/op
[info] VarIntEncodingTest.testEncode   avgt    5   114.477 ±  0.822  ns/op
```

scala 3.8.2 / java 25

```
[info] Benchmark                       Mode  Cnt     Score    Error  Units
[info] BitOpsTest.testRead             avgt    5   111.510 ±  1.612  ns/op
[info] HuffmanPgnBench.decode          avgt    5  3016.751 ± 74.423  us/op
[info] HuffmanPgnBench.encode          avgt    5  2858.993 ± 12.959  us/op
[info] LinearEstimateTest.testEncode   avgt    5    33.365 ±  0.856  ns/op
[info] LowBitTruncTest.testEncode      avgt    5    17.888 ±  0.074  ns/op
[info] OverallEncodingTest.testDecode  avgt    5   131.078 ±  1.630  ns/op
[info] OverallEncodingTest.testEncode  avgt    5   161.584 ±  5.943  ns/op
[info] VarIntEncodingTest.testDecode   avgt    5    84.395 ±  1.937  ns/op
[info] VarIntEncodingTest.testEncode   avgt    5   115.358 ±  1.222  ns/op
```
