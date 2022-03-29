using System;
using BenchmarkDotNet.Running;
using plexMovieFolders.Benchmark.Benchmarks;

namespace plexMovieFolders.Benchmark
{
    class Program
    {
        static void Main(string[] args)
        {
            BenchmarkRunner.Run<ClassBenchmarks>();
        }
    }
}
