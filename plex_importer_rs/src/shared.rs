
pub trait Shared {
    fn start(&mut self);

    fn build_destination_path(&mut self);

    fn build_filename(&mut self);

    fn check_for_duplicate(&self);

    fn move_operation(&self);

    fn remove_root_dir(&self);
}

