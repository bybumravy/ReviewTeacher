import { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { getReviewerToken } from '../utils/cookie';
import { getGateStatus } from '../api/gateApi';

const GateContext = createContext(null);

const EMPTY_GATE = {
  creditBalance: 0,
  pendingReviews: 0,
  totalReviews: 0,
  unlockedTeacherIds: [],
};

export function GateProvider({ children }) {
  const [gate, setGate] = useState(EMPTY_GATE);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const token = getReviewerToken();
    if (!token) {
      setGate(EMPTY_GATE);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const data = await getGateStatus();
      setGate({
        creditBalance: data.creditBalance ?? 0,
        pendingReviews: data.pendingReviews ?? 0,
        totalReviews: data.totalReviews ?? 0,
        unlockedTeacherIds: data.unlockedTeacherIds ?? [],
      });
    } catch {
      // Silent fail — keep previous state, UI falls back gracefully
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const isUnlocked = useCallback((teacherId) => {
    return gate.unlockedTeacherIds.includes(teacherId);
  }, [gate.unlockedTeacherIds]);

  const hasCredit = gate.creditBalance > 0;

  return (
    <GateContext.Provider value={{ gate, loading, refresh, isUnlocked, hasCredit }}>
      {children}
    </GateContext.Provider>
  );
}

export function useGate() {
  const ctx = useContext(GateContext);
  if (!ctx) throw new Error('useGate must be used within GateProvider');
  return ctx;
}
