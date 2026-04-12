import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, test, vi } from 'vitest';
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
  createImportEventSource: vi.fn(),
}));

vi.mock('./api', () => ({
  ...apiMocks,
}));

class FakeEventSource {
  constructor() {
    this.listeners = new Map();
  }

  addEventListener(type, handler) {
    this.listeners.set(type, handler);
  }

  removeEventListener(type) {
    this.listeners.delete(type);
  }

  emit(type, payload) {
    const handler = this.listeners.get(type);
    if (handler) {
      handler({ data: JSON.stringify(payload) });
    }
  }

  close() {}
}

let eventSource;
const originalEventSource = globalThis.EventSource;

beforeEach(() => {
  eventSource = new FakeEventSource();
  apiMocks.createImportEventSource.mockReturnValue(eventSource);
  globalThis.EventSource = FakeEventSource;
});

afterEach(() => {
  vi.clearAllMocks();
  globalThis.EventSource = originalEventSource;
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

test('shows a toast when an import-complete event arrives', async () => {
  render(<App />);

  await waitFor(() => screen.getByText('dir1'));

  await act(async () => {
    eventSource.emit('import-complete', {
      status: 'MOVED',
      destinationPath: '/dest/Movies/Dune (2021)/Dune (2021).mkv',
      sourceDirectoryDeleted: true,
      message: 'Import completed successfully.',
    });
  });

  await waitFor(() => screen.getByRole('status'));
  expect(screen.getByText('Import finished')).toBeInTheDocument();
  expect(screen.getByText('Import completed successfully.')).toBeInTheDocument();
});
