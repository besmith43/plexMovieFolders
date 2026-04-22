import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, test, vi } from 'vitest';
import App from './App';

const apiMocks = vi.hoisted(() => ({
  fetchDirectories: vi.fn(async () => [
    {
      name: 'dir1',
      absolutePath: '/source/dir1',
      videoFiles: [{ fileName: 'movie.mkv', extension: 'mkv', absolutePath: '/source/dir1/movie.mkv' }],
    },
  ]),
  fetchTvSeries: vi.fn(async () => ['__NEW_SERIES__', 'Eureka']),
  previewImport: vi.fn(async () => ({
    sourceFile: 'movie.mkv',
    destinationPath: '/dest/Movies/Dune (2021)/Dune (2021).mkv',
    collision: false,
    message: 'Destination path is available.',
  })),
  executeImport: vi.fn(async () => ({
    status: 'MOVED',
    destinationPath: '/dest/Movies/Dune (2021)/Dune (2021).mkv',
    sourceDirectoryDeleted: true,
    message: 'Import completed successfully.',
  })),
}));

vi.mock('./api', () => ({
  ...apiMocks,
}));

afterEach(() => {
  vi.clearAllMocks();
});

test('renders workflow and previews a movie import', async () => {
  render(<App />);

  await waitFor(() => screen.getByText('dir1'));

  fireEvent.click(screen.getByText('dir1'));
  fireEvent.change(screen.getByLabelText('Video file'), { target: { value: 'movie.mkv' } });
  fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Dune' } });
  fireEvent.change(screen.getByLabelText('Year'), { target: { value: '2021' } });
  fireEvent.click(screen.getByText('Preview destination'));

  await waitFor(() => screen.getByText('/dest/Movies/Dune (2021)/Dune (2021).mkv'));
});
