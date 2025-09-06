// use std::path::PathBuf;

use indicatif::{ProgressBar, ProgressStyle};
use std::fs::{self, File};
use std::io::{self, BufReader, BufWriter, Read, Write};
use std::path::{Path, PathBuf};

use crate::shared::Shared;

pub struct TvShow {
    pub source: PathBuf,
    pub destination: PathBuf,
    pub series_name: String,
    pub season_number: u32,
    pub episode_number: u32,
    pub episode_title: Option<String>,
}

impl TvShow {
    // this needs to take the season number and turn it into a string that looks like xx
    fn convert_number(&self, number: u32) -> String {
        if number < 10 {
            format!("0{}", number)
        } else {
            format!("{}", number)
        }
    }
}

impl Shared for TvShow {
    fn start(&mut self) {
        self.build_destination_path();

        self.build_filename();

        self.check_for_duplicate();

        self.move_operation();

        self.remove_root_dir();

        clearscreen::clear().unwrap();
    }

    // use this to overwrite the dest_dir with the new root dir created
    fn build_destination_path(&mut self) {
        let root_dir = format!(
            "{}/{}/Season {}",
            self.destination.to_str().unwrap(),
            &self.series_name,
            self.convert_number(self.season_number.clone())
        );

        println!("Make TV Show directory at {}", root_dir);
        let _ = fs_extra::dir::create_all(&root_dir, false);

        self.destination = PathBuf::from(root_dir);
    }

    // this depends on build_destination_path
    // also overwrites the dest_dir
    fn build_filename(&mut self) {
        let filename: String;

        if self.episode_title.is_some() {
            filename = format!(
                "{}/{} - s{}e{} - {}.{}",
                &self.destination.to_str().unwrap(),
                &self.series_name,
                self.convert_number(self.season_number),
                self.convert_number(self.episode_number),
                self.episode_title.as_ref().unwrap(),
                &self.source.extension().unwrap().to_str().unwrap()
            );
        } else {
            filename = format!(
                "{}/{} - s{}e{}.{}",
                &self.destination.to_str().unwrap(),
                &self.series_name,
                self.convert_number(self.season_number.clone()),
                self.convert_number(self.episode_number.clone()),
                &self.source.extension().unwrap().to_str().unwrap()
            );
        }

        println!("filename generated {}", &filename);

        self.destination = PathBuf::from(filename);
    }

    fn check_for_duplicate(&self) {
        if self.destination.exists() {
            println!("quitting application");
            std::process::exit(0);
        }
    }

    fn move_operation(&self) {
        // println!(
        // "Moving file from\n\t{}\nto\n\t{}",
        // &self.source.to_str().unwrap(),
        // &self.destination.to_str().unwrap()
        // );
        // let c_options = fs_extra::file::CopyOptions::new();
        // let _ = fs_extra::file::move_file(&self.source, &self.destination, &c_options);

        // 1. Get the size of the source file to set the progress bar length.
        let file_size = fs::metadata(&self.source).unwrap().len();

        // 2. Set up the progress bar.
        let pb = ProgressBar::new(file_size);
        pb.set_style(ProgressStyle::default_bar()
        .template("{spinner:.green} [{elapsed_precise}] [{bar:40.cyan/blue}] {bytes}/{total_bytes} ({eta})")
        .unwrap()
        .progress_chars("#>-"));

        // 3. Open the source file for reading.
        let mut source_file = BufReader::new(File::open(&self.source).unwrap());

        // 4. Create and open the destination file for writing.
        let mut dest_file = BufWriter::new(File::create(&self.destination).unwrap());

        // 5. Copy the data in chunks and update the progress bar.
        let mut buffer = [0; 8192]; // 8 KB buffer for copying chunks
        loop {
            let bytes_read = source_file.read(&mut buffer).unwrap();
            if bytes_read == 0 {
                break;
            }
            dest_file.write_all(&buffer[..bytes_read]).unwrap();
            pb.inc(bytes_read as u64);
        }

        // 6. Finish the progress bar and clear it from the terminal.
        pb.finish_and_clear();

        // 7. Close the files and remove the original source file.
        // The File and BufWriter destructors handle closing, but we must
        // explicitly remove the original file to complete the "move" operation.
        fs::remove_file(&self.source).unwrap();

        // Ok(())
    }

    fn remove_root_dir(&self) {
        println!(
            "Deleting parent folder {}",
            self.source.parent().unwrap().to_str().unwrap()
        );
        let _ = fs_extra::dir::remove(self.source.parent().unwrap());
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tv_show() {
        ()
    }

    #[test]
    fn test_tv_convert() {
        let test_tv_show = TvShow {
            source: PathBuf::from(""),
            destination: PathBuf::from(""),
            series_name: "".to_string(),
            season_number: 1,
            episode_number: 10,
            episode_title: Some("test".to_string()),
        };

        assert_eq!(test_tv_show.convert_number(2), "02");
        assert_ne!(test_tv_show.convert_number(5), "5");
        assert_eq!(test_tv_show.convert_number(10), "10");
    }
}
