#!/usr/bin/env python3

import sys
from os import listdir
from os.path import isfile, isdir, join

def main():
    n = len(sys.argv)

    if n < 3:
        print("you need to supply a source and destination")
        return
    elif n > 3:
        print("you supplied too many arguments")
        return

    source = sys.argv[1]

    print("Source: ", source)

    destination = sys.argv[2]

    print("Destination: ", destination)


    dirs = []

    # print(listdir(source))

    for item in listdir(source):
        if isdir(join(source,item)):
            dirs.append(item)

    # print(dirs)


def old_func():
    mypath = "."

    onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]

    print(onlyfiles)




def my_function():
    n = len(sys.argv)
    print("Total arguments passed:", n)

    # Arguments passed
    print("\nName of Python script:", sys.argv[0])

    print("\nArguments passed:", end = " ")
    for i in range(1, n):
        print(sys.argv[i], end = " ")

    # Addition of numbers
    Sum = 0
    # Using argparse module
    for i in range(1, n):
        Sum += int(sys.argv[i])

    print("\n\nResult:", Sum)





if __name__ == "__main__":
    main()

