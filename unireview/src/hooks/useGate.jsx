import { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { getReviewerToken } from '../utils/cookie';

const GateContext = createContext(null);

const MOCK_GATE = {
  creditBalance: 0,
  pendingReviews: 0,
  totalReviews: 0,
  unlockedTeacherIds: [],
};

export function GateProvider({ children }) {
  const [gate, setGate] = useState(MOCK_GATE);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const token = getReviewerToken();
    if (!token) {
      setGate(MOCK_GATE);
      setLoading(false);
      return;
    }

    try {
      // In production, this calls the API:
      // const data = await getGateStatus();
      // setGate(data);

      // For now, read from localStorage mock
      const stored = localStorage.getItem('gate_status');
      if (stored) {
        setGate(JSON.parse(stored));
      }
    } catch {
      // Silent fail
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

  const addCredit = useCallback((amount = 1) => {
    setGate(prev => {
      const updated = { ...prev, creditBalance: prev.creditBalance + amount };
      localStorage.setItem('gate_status', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const spendCredit = useCallback((teacherId) => {
    setGate(prev => {
      const updated = {
        ...prev,
        creditBalance: prev.creditBalance - 1,
        unlockedTeacherIds: [...prev.unlockedTeacherIds, teacherId],
      };
      localStorage.setItem('gate_status', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const unlockTeacher = useCallback((teacherId) => {
    setGate(prev => {
      if (prev.unlockedTeacherIds.includes(teacherId)) return prev;
      const updated = {
        ...prev,
        unlockedTeacherIds: [...prev.unlockedTeacherIds, teacherId],
      };
      localStorage.setItem('gate_status', JSON.stringify(updated));
      return updated;
    });
  }, []);

  return (
    <GateContext.Provider value={{ gate, loading, refresh, isUnlocked, hasCredit, addCredit, spendCredit, unlockTeacher }}>
      {children}
    </GateContext.Provider>
  );
}

export function useGate() {
  const ctx = useContext(GateContext);
  if (!ctx) throw new Error('useGate must be used within GateProvider');
  return ctx;
}
