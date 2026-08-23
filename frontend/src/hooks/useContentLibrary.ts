import { useCallback, useEffect, useRef, useState } from 'react';
import {
  createContent,
  getContent,
  getContents,
  updateContent,
} from '../api/contentApi';
import type {
  ContentDraft,
  KnowledgeContent,
  KnowledgeContentSummary,
} from '../domain/content';
import { isAbortError, toErrorMessage } from '../utils/errors';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function useContentLibrary() {
  const [contents, setContents] = useState<KnowledgeContentSummary[]>([]);
  const [selectedContent, setSelectedContent] = useState<KnowledgeContent | null>(null);
  const [listStatus, setListStatus] = useState<LoadStatus>('idle');
  const [detailStatus, setDetailStatus] = useState<LoadStatus>('idle');
  const [saveStatus, setSaveStatus] = useState<LoadStatus>('idle');
  const [listError, setListError] = useState<string | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const listControllerRef = useRef<AbortController | null>(null);
  const detailControllerRef = useRef<AbortController | null>(null);

  const loadContents = useCallback(async () => {
    listControllerRef.current?.abort();
    const controller = new AbortController();
    listControllerRef.current = controller;
    setListStatus('loading');
    setListError(null);
    try {
      const loaded = await getContents(controller.signal);
      if (controller.signal.aborted) return;
      setContents(loaded);
      setListStatus('success');
    } catch (error) {
      if (isAbortError(error)) return;
      setListError(toErrorMessage(error));
      setListStatus('error');
    }
  }, []);

  useEffect(() => {
    void loadContents();
    return () => {
      listControllerRef.current?.abort();
      detailControllerRef.current?.abort();
    };
  }, [loadContents]);

  const openContent = useCallback(async (id: string) => {
    detailControllerRef.current?.abort();
    const controller = new AbortController();
    detailControllerRef.current = controller;
    setSelectedContent(null);
    setDetailStatus('loading');
    setDetailError(null);
    try {
      const loaded = await getContent(id, controller.signal);
      if (controller.signal.aborted) return;
      setSelectedContent(loaded);
      setDetailStatus('success');
    } catch (error) {
      if (isAbortError(error)) return;
      setDetailError(toErrorMessage(error));
      setDetailStatus('error');
    }
  }, []);

  const saveContent = useCallback(
    async (draft: ContentDraft, id?: string): Promise<KnowledgeContent | null> => {
      setSaveStatus('loading');
      setSaveError(null);
      try {
        const saved = id ? await updateContent(id, draft) : await createContent(draft);
        setSelectedContent(saved);
        setDetailStatus('success');
        setDetailError(null);
        setSaveStatus('success');
        void loadContents();
        return saved;
      } catch (error) {
        setSaveError(toErrorMessage(error));
        setSaveStatus('error');
        return null;
      }
    },
    [loadContents],
  );

  return {
    contents,
    selectedContent,
    listStatus,
    detailStatus,
    saveStatus,
    listError,
    detailError,
    saveError,
    loadContents,
    openContent,
    saveContent,
  };
}
