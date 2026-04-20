import { useCallback, useEffect, useRef } from 'react';

interface UsePollingTaskOptions {
  enabled: boolean;
  intervalMs: number;
  immediate?: boolean;
  pauseWhenHidden?: boolean;
}

export function usePollingTask(task: () => Promise<void> | void, options: UsePollingTaskOptions) {
  const { enabled, intervalMs, immediate = true, pauseWhenHidden = true } = options;
  const taskRef = useRef(task);
  const runningRef = useRef(false);

  taskRef.current = task;

  const runNow = useCallback(async () => {
    if (!enabled || runningRef.current) {
      return;
    }
    if (pauseWhenHidden && document.visibilityState === 'hidden') {
      return;
    }
    runningRef.current = true;
    try {
      await taskRef.current();
    } finally {
      runningRef.current = false;
    }
  }, [enabled, pauseWhenHidden]);

  useEffect(() => {
    if (!enabled) {
      return;
    }
    let timer: number | undefined;
    let disposed = false;

    const schedule = () => {
      if (disposed) {
        return;
      }
      timer = window.setTimeout(async () => {
        await runNow();
        schedule();
      }, intervalMs);
    };

    if (immediate) {
      void runNow();
    }
    schedule();

    return () => {
      disposed = true;
      if (timer) {
        window.clearTimeout(timer);
      }
    };
  }, [enabled, immediate, intervalMs, runNow]);

  useEffect(() => {
    if (!enabled || !pauseWhenHidden) {
      return;
    }
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void runNow();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [enabled, pauseWhenHidden, runNow]);

  return { runNow };
}
