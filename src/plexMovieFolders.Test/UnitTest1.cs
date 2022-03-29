using System;
using Xunit;
using plexMovieFolders.Class;

namespace plexMovieFolders.Test
{
    public class UnitTest1
    {
        [Fact]
        public void Test1()
        {
            Class1 testClass = new Class1();
            int result = testClass.Add(2,3);
            Assert.Equal(5, result);
        }
    }
}
