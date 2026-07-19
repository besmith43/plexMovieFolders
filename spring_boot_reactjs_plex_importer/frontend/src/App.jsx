import { useEffect, useMemo, useRef, useState } from 'react';
import { executeImport, fetchDirectories, fetchTvSeries, previewImport } from './api';

const NEW_SERIES = '__NEW_SERIES__';

const initialForm = {
  contentType: 'MOVIE',
  sourceDirectory: '',
  sourceFile: '',
  title: '',
  year: '',
  standardEdition: true,
  edition: '',
  existingSeries: NEW_SERIES,
  newSeriesName: '',
  seasonNumber: '',
  episodeNumber: '',
};

function App() {
  const [directories, setDirectories] = useState([]);
  const [tvSeries, setTvSeries] = useState([NEW_SERIES]);
  const [form, setForm] = useState(initialForm);
  const [preview, setPreview] = useState(null);
  const [conflictAction, setConflictAction] = useState('SKIP');
  const [busy, setBusy] = useState(true);
  const [previewBusy, setPreviewBusy] = useState(false);
  const [submitBusy, setSubmitBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [toast, setToast] = useState(null);
  const toastTimerRef = useRef(null);

  useEffect(() => {
    loadInitialData();
  }, []);

  async function loadInitialData() {
    setBusy(true);
    setError('');
    try {
      const [directoryData, seriesData] = await Promise.all([fetchDirectories(), fetchTvSeries()]);
      setDirectories(directoryData);
      setTvSeries(seriesData.length ? seriesData : [NEW_SERIES]);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setBusy(false);
    }
  }

  const selectedDirectory = useMemo(
    () => directories.find((directory) => directory.absolutePath === form.sourceDirectory) || null,
    [directories, form.sourceDirectory],
  );

  function resetWorkflow(nextDirectory, nextFile) {
    setForm((current) => ({
      ...initialForm,
      sourceDirectory: nextDirectory ?? current.sourceDirectory,
      sourceFile: nextFile ?? current.sourceFile,
      contentType: current.contentType,
      existingSeries: NEW_SERIES,
    }));
    setPreview(null);
    setConflictAction('SKIP');
    setError('');
  }

  function updateField(name, value) {
    setForm((current) => ({ ...current, [name]: value }));
    setPreview(null);
    setResult(null);
    if (name === 'contentType') {
      setConflictAction('SKIP');
    }
  }

  function clearToastTimer() {
    if (toastTimerRef.current !== null) {
      window.clearTimeout(toastTimerRef.current);
      toastTimerRef.current = null;
    }
  }

  async function handlePreview(event) {
    event.preventDefault();
    setPreviewBusy(true);
    setError('');
    setResult(null);
    try {
      const payload = buildPayload(form);
      const previewResponse = await previewImport(payload);
      setPreview(previewResponse);
      setConflictAction('SKIP');
    } catch (previewError) {
      setError(previewError.message);
    } finally {
      setPreviewBusy(false);
    }
  }

  async function handleImport() {
    setSubmitBusy(true);
    setError('');
    try {
      const payload = buildPayload(form);
      const importResponse = await executeImport({ preview: payload, conflictAction });
      setResult(importResponse);
      setToast(importResponse);
      clearToastTimer();
      toastTimerRef.current = window.setTimeout(() => setToast(null), 5000);
      setPreview(null);
      resetWorkflow('', '');
      await loadInitialData();
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setSubmitBusy(false);
    }
  }

  return (
    <div className="page-shell">
      <main className="app-card">
        <section className="hero">
          <p className="eyebrow">Plex Library Intake</p>
          <h1>Route finished downloads into the right Plex library path.</h1>
          <p className="hero-copy">
            Pick a source directory, choose one video file, describe it as a movie or TV episode, then confirm the exact target path before anything moves.
          </p>
        </section>

        {error ? <div className="banner error">{error}</div> : null}
        {result ? <div className="banner success">{result.message}</div> : null}
        {toast ? (
          <div className={`toast ${toast.status === 'MOVED' ? 'success' : 'warning'}`} role="status" aria-live="polite">
            <strong>{toast.status === 'MOVED' ? 'Import finished' : 'Import skipped'}</strong>
            <span>{toast.message}</span>
          </div>
        ) : null}

        <section className="layout-grid">
          <aside className="panel source-panel">
            <div className="panel-header">
              <h2>1. Source directories</h2>
              <button type="button" className="ghost-button" onClick={loadInitialData} disabled={busy}>
                Refresh
              </button>
            </div>
            {busy ? <p>Scanning source tree…</p> : null}
            {!busy && directories.length === 0 ? <p>No directories with supported video files were found.</p> : null}
            <div className="directory-list" aria-label="Source directories list">
              {directories.map((directory) => {
                const isSelected = directory.absolutePath === form.sourceDirectory;
                return (
                  <button
                    key={directory.absolutePath}
                    type="button"
                    className={`directory-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => resetWorkflow(directory.absolutePath, '')}
                  >
                    <span>{directory.name}</span>
                    <small>{directory.videoFiles.length} video file(s)</small>
                  </button>
                );
              })}
            </div>
          </aside>

          <section className="panel workflow-panel">
            <form onSubmit={handlePreview}>
              <div className="step-block">
                <h2>2. Select file</h2>
                <div className="field-group">
                  <label htmlFor="sourceFile">Video file</label>
                  <select
                    id="sourceFile"
                    value={form.sourceFile}
                    onChange={(event) => updateField('sourceFile', event.target.value)}
                    disabled={!selectedDirectory}
                  >
                    <option value="">Select a video file</option>
                    {selectedDirectory?.videoFiles.map((file) => (
                      <option key={file.absolutePath} value={file.fileName}>
                        {file.fileName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="step-block">
                <h2>3. Content type</h2>
                <div className="segmented-control">
                  <button
                    type="button"
                    className={form.contentType === 'MOVIE' ? 'active' : ''}
                    onClick={() => updateField('contentType', 'MOVIE')}
                  >
                    Movie
                  </button>
                  <button
                    type="button"
                    className={form.contentType === 'TV_SHOW' ? 'active' : ''}
                    onClick={() => updateField('contentType', 'TV_SHOW')}
                  >
                    TV Show
                  </button>
                </div>
              </div>

              {form.contentType === 'MOVIE' ? (
                <MovieFields form={form} updateField={updateField} />
              ) : (
                <TvFields form={form} updateField={updateField} tvSeries={tvSeries} />
              )}

              <div className="step-block actions">
                <button type="submit" className="primary-button" disabled={previewBusy || !form.sourceDirectory || !form.sourceFile}>
                  {previewBusy ? 'Calculating…' : 'Preview destination'}
                </button>
              </div>
            </form>

            <section className="step-block confirmation-block">
              <h2>4. Confirm final path</h2>
              {preview ? (
                <>
                  <div className="preview-box">{preview.destinationPath}</div>
                  <p className="muted-copy">{preview.message}</p>
                  {preview.collision ? (
                    <div className="field-group">
                      <label htmlFor="conflictAction">If destination exists</label>
                      <select
                        id="conflictAction"
                        value={conflictAction}
                        onChange={(event) => setConflictAction(event.target.value)}
                      >
                        <option value="SKIP">Skip</option>
                        <option value="OVERWRITE">Overwrite</option>
                      </select>
                    </div>
                  ) : null}
                  <button type="button" className="primary-button" onClick={handleImport} disabled={submitBusy}>
                    {submitBusy ? 'Importing…' : 'Execute import'}
                  </button>
                </>
              ) : (
                <p>Generate a preview to confirm the resolved Plex destination path.</p>
              )}
            </section>
          </section>
        </section>
      </main>
    </div>
  );
}

function MovieFields({ form, updateField }) {
  return (
    <div className="step-block">
      <h2>Movie details</h2>
      <div className="field-grid">
        <div className="field-group">
          <label htmlFor="title">Title</label>
          <input id="title" value={form.title} onChange={(event) => updateField('title', event.target.value)} />
        </div>
        <div className="field-group">
          <label htmlFor="year">Year</label>
          <input id="year" type="number" min="1901" value={form.year} onChange={(event) => updateField('year', event.target.value)} />
        </div>
      </div>
      <div className="field-group inline-check">
        <label htmlFor="standardEdition">Standard edition</label>
        <input
          id="standardEdition"
          type="checkbox"
          checked={form.standardEdition}
          onChange={(event) => updateField('standardEdition', event.target.checked)}
        />
      </div>
      {!form.standardEdition ? (
        <div className="field-group">
          <label htmlFor="edition">Edition</label>
          <input id="edition" value={form.edition} onChange={(event) => updateField('edition', event.target.value)} />
        </div>
      ) : null}
    </div>
  );
}

function TvFields({ form, updateField, tvSeries }) {
  return (
    <div className="step-block">
      <h2>TV details</h2>
      <div className="field-group">
        <label htmlFor="existingSeries">Series</label>
        <select id="existingSeries" value={form.existingSeries} onChange={(event) => updateField('existingSeries', event.target.value)}>
          {tvSeries.map((series) => (
            <option key={series} value={series}>
              {series === NEW_SERIES ? 'New Series' : series}
            </option>
          ))}
        </select>
      </div>
      {form.existingSeries === NEW_SERIES ? (
        <div className="field-group">
          <label htmlFor="newSeriesName">New series name</label>
          <input id="newSeriesName" value={form.newSeriesName} onChange={(event) => updateField('newSeriesName', event.target.value)} />
        </div>
      ) : null}
      <div className="field-grid">
        <div className="field-group">
          <label htmlFor="seasonNumber">Season</label>
          <input
            id="seasonNumber"
            type="number"
            min="1"
            value={form.seasonNumber}
            onChange={(event) => updateField('seasonNumber', event.target.value)}
          />
        </div>
        <div className="field-group">
          <label htmlFor="episodeNumber">Episode</label>
          <input
            id="episodeNumber"
            type="number"
            min="1"
            value={form.episodeNumber}
            onChange={(event) => updateField('episodeNumber', event.target.value)}
          />
        </div>
      </div>
    </div>
  );
}

function buildPayload(form) {
  return {
    sourceDirectory: form.sourceDirectory,
    sourceFile: form.sourceFile,
    contentType: form.contentType,
    title: form.contentType === 'MOVIE' ? form.title : null,
    year: form.contentType === 'MOVIE' && form.year ? Number(form.year) : null,
    standardEdition: form.contentType === 'MOVIE' ? form.standardEdition : null,
    edition: form.contentType === 'MOVIE' && !form.standardEdition ? form.edition : null,
    existingSeries: form.contentType === 'TV_SHOW' ? form.existingSeries : null,
    newSeriesName: form.contentType === 'TV_SHOW' && form.existingSeries === NEW_SERIES ? form.newSeriesName : null,
    seasonNumber: form.contentType === 'TV_SHOW' && form.seasonNumber ? Number(form.seasonNumber) : null,
    episodeNumber: form.contentType === 'TV_SHOW' && form.episodeNumber ? Number(form.episodeNumber) : null,
  };
}

export default App;
