//
//  ContentView.swift
//  pi
//
//  Created by Blake Smith on 8/24/25.
//

import SwiftUI

struct UserSelections {
    var source: URL?
    var movie_dest: URL?
    var tv_dest: URL?
    
    init() {
        self.source = nil
        self.movie_dest = nil
        self.tv_dest = nil
    }
}

struct CheckBoxURL: Identifiable {
    let id = UUID()
    var url: URL
    var isSelected: Bool = false
}

struct ContentView: View {
    @State private var userSelections: UserSelections = UserSelections()
    @State private var options: [CheckBoxURL] = []
    @State private var sourceShowFileImporter = false
    @State private var movieShowFileImporter = false
    @State private var tvShowFileImporter = false

    var body: some View {
        VStack {
            Text("Settings")
                .padding()
            HStack {
                Text("Source: \(userSelections.source?.absoluteString)")
                Button("Select")
                {
                    sourceShowFileImporter = true
                }
                .fileImporter(
                    isPresented: $sourceShowFileImporter,
                    allowedContentTypes: [.directory], // Specify .directory for folder selection
                    allowsMultipleSelection: false // Set to true if multiple directories can be selected
                ) { result in
                    switch result {
                    case .success(let urls):
                        if let url = urls.first {
                            userSelections.source = url
                            print("Selected directory: \(url.absoluteString)")
                            options = getListOfContents(documentsURL: url)
                            // You can now work with the selectedDirectoryURL
                        }
                    case .failure(let error):
                        print("Error selecting directory: \(error.localizedDescription)")
                    }
                }
            }
            Text("Movie Destination: \(userSelections.movie_dest?.absoluteString)")
            Text("TV Show Destination: \(userSelections.tv_dest?.absoluteString)")
        }
        VStack {
            Text("App")
                .padding()
            ForEach($options) { $option in
                HStack {
                    Toggle(option.url.absoluteString, isOn: $option.isSelected)
                        .toggleStyle(.checkbox)
                }
            }
        }
    }
}

#Preview {
    ContentView()
}



func getListOfContents(documentsURL: URL) -> [CheckBoxURL] {
    // let documentsURL: URL = URL(fileURLWithPath: "/Users/besmith/Developer/Personal/plexMovieFolders/swift_pi/src")
    // let documentsURL2: URL = URL(fileURLWithPath: "file:///Users/besmith/Developer/Personal/plexMovieFolders/swift_pi/src")
    var checkBoxList: [CheckBoxURL] = []
    
    do {
        let contents = try FileManager.default.contentsOfDirectory(at: documentsURL, includingPropertiesForKeys: [.isDirectoryKey], options: .skipsHiddenFiles)
        
        for fileURL in contents {
            print(fileURL.path)
            checkBoxList.append(CheckBoxURL(url: fileURL))
        }
    } catch {
        print("Error enumerating directory contents: \(error)")
    }

    return checkBoxList
}
