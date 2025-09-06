//
//  main.swift
//  pi
//
//  Created by Blake Smith on 9/6/25.
//

import Foundation
@preconcurrency import Swiftline

print("Hello, World!")

#if DEBUG
let src = "/Users/besmith/Developer/Personal/plexMovieFolders/swift_pi_cli/src"
let movies_dest = "/Users/besmith/Developer/Personal/plexMovieFolders/swift_pi_cli/dest/Movies"
let tv_dest = "/Users/besmith/Developer/Personal/plexMovieFolders/swift_pi_cli/dest/TV Shows"
#else
let src = "/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete"
let movies_dest = "/Volumes/Content_Vault/Plex/Movies"
let tv_dest = "/Volumes/Content_Vault/Plex/TV Shows"
#endif

try listSubdirectories(atPath: src).forEach { processDir($0) }


func processDir(_ dir: String) {
    print(dir)
    
    let type_choice = choose("Is \(dir) a movie or TV show? ", choices: "Movies", "TV Shows", "Skip", "Quit")
}

func listSubdirectories(atPath path: String) throws -> [String] {
    let fileManager = FileManager.default
    var subdirectories: [String] = []

    do {
        let contents = try fileManager.contentsOfDirectory(atPath: path)
        for item in contents {
            let itemPath = (path as NSString).appendingPathComponent(item)
            var isDirectory: ObjCBool = false
            if fileManager.fileExists(atPath: itemPath, isDirectory: &isDirectory) {
                if isDirectory.boolValue {
                    subdirectories.append(item)
                }
            }
        }
    } catch {
        throw error // Re-throw any errors encountered during directory enumeration
    }
    return subdirectories
}

