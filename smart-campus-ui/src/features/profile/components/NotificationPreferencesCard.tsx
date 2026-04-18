import { useEffect, useState } from 'react';
import {
  notificationService,
  type NotificationPreferenceDTO,
} from '@/services/notificationService';

type Channel = {
  key: 'inApp' | 'email' | 'push';
  label: string;
  description: string;
  lockedOn?: boolean;
};

const CHANNELS: Channel[] = [
  {
    key: 'inApp',
    label: 'In-app',
    description: 'Bell icon and notifications page. Always on so you never miss an update.',
    lockedOn: true,
  },
  {
    key: 'email',
    label: 'Email',
    description: 'Delivered to your registered email inbox.',
  },
  {
    key: 'push',
    label: 'Browser push',
    description: 'Desktop/mobile browser alert, even when the tab is closed. Requires permission.',
  },
];

export default function NotificationPreferencesCard() {
  const [prefs, setPrefs] = useState<NotificationPreferenceDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<'email' | 'push' | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    notificationService
      .getPreferences()
      .then((res) => setPrefs(res.data))
      .catch(() => setError('Failed to load preferences.'))
      .finally(() => setLoading(false));
  }, []);

  const toggle = async (key: 'email' | 'push') => {
    if (!prefs) return;
    const next = { ...prefs, [key]: !prefs[key] };
    setSaving(key);
    setPrefs(next);
    try {
      await notificationService.updatePreferences({ [key]: next[key] });
    } catch {
      setPrefs(prefs); // revert
      setError('Failed to save preference.');
    } finally {
      setSaving(null);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-100 p-6">
      <div className="mb-5">
        <h2 className="text-lg font-bold text-campus-900">Notification preferences</h2>
        <p className="text-xs text-gray-500 mt-1">
          Choose how you want to receive notifications. In-app alerts are always on — email and browser push can be turned off.
        </p>
      </div>

      {error && (
        <div className="mb-4 p-2.5 rounded-lg bg-red-50 border border-red-100 text-red-600 text-xs font-medium">
          {error}
        </div>
      )}

      {loading || !prefs ? (
        <div className="flex justify-center py-8">
          <div className="w-6 h-6 border-2 border-gray-200 border-t-campus-600 rounded-full animate-spin" />
        </div>
      ) : (
        <div className="divide-y divide-gray-50">
          {CHANNELS.map((ch) => {
            const on = ch.lockedOn ? true : prefs[ch.key as 'email' | 'push'];
            const isSaving = saving === ch.key;
            return (
              <div key={ch.key} className="flex items-start justify-between gap-4 py-3 first:pt-0 last:pb-0">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-campus-900">{ch.label}</span>
                    {ch.lockedOn && (
                      <span className="text-[10px] font-bold uppercase tracking-wider text-campus-600 bg-campus-50 rounded px-1.5 py-0.5">
                        Always on
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mt-0.5">{ch.description}</p>
                </div>
                <label
                  className={`shrink-0 inline-flex items-center gap-2 ${
                    ch.lockedOn ? 'cursor-not-allowed' : 'cursor-pointer'
                  } ${isSaving ? 'opacity-60' : ''}`}
                >
                  <input
                    type="checkbox"
                    checked={on}
                    disabled={ch.lockedOn || isSaving}
                    onChange={() => !ch.lockedOn && toggle(ch.key as 'email' | 'push')}
                    className="w-5 h-5 rounded border-gray-300 text-campus-600 focus:ring-campus-500 focus:ring-offset-0 disabled:opacity-60 cursor-pointer disabled:cursor-not-allowed"
                  />
                  <span className={`text-xs font-semibold ${on ? 'text-campus-800' : 'text-gray-400'}`}>
                    {on ? 'On' : 'Off'}
                  </span>
                </label>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
