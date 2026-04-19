import { useEffect, useState } from 'react';
import {
  notificationService,
  type NotificationPreferenceDTO,
} from '@/services/notificationService';
import { disablePush, enablePush, getCurrentState, pushSupported, type PushState } from '@/lib/push';

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

function pushStatusText(state: PushState, pushPrefOn: boolean): string | null {
  if (state === 'unsupported') return 'Your browser doesn\'t support push notifications.';
  if (state === 'denied') return 'Notifications are blocked in your browser settings.';
  if (pushPrefOn && state === 'granted-unsubscribed') return 'Subscription lost — toggle off and back on to re-subscribe.';
  return null;
}

export default function NotificationPreferencesCard() {
  const [prefs, setPrefs] = useState<NotificationPreferenceDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<'email' | 'push' | 'quiet' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pushState, setPushState] = useState<PushState>('default');
  // Draft values for the time fields so users can edit freely and commit with Save.
  const [draftStart, setDraftStart] = useState('');
  const [draftEnd, setDraftEnd] = useState('');

  useEffect(() => {
    notificationService
      .getPreferences()
      .then((res) => {
        setPrefs(res.data);
        setDraftStart(res.data.quietHoursStart);
        setDraftEnd(res.data.quietHoursEnd);
      })
      .catch(() => setError('Failed to load preferences.'))
      .finally(() => setLoading(false));

    if (pushSupported()) {
      void getCurrentState().then(setPushState);
    } else {
      setPushState('unsupported');
    }
  }, []);

  const toggleEmail = async () => {
    if (!prefs) return;
    const next = !prefs.email;
    setSaving('email');
    setPrefs({ ...prefs, email: next });
    try {
      await notificationService.updatePreferences({ email: next });
    } catch {
      setPrefs({ ...prefs });
      setError('Failed to save preference.');
    } finally {
      setSaving(null);
    }
  };

  const togglePush = async () => {
    if (!prefs) return;
    const turningOn = !prefs.push;
    setSaving('push');
    setError(null);

    try {
      if (turningOn) {
        const newState = await enablePush();
        setPushState(newState);
        if (newState !== 'granted-subscribed') {
          // Permission denied or unsupported — keep the preference off.
          if (newState === 'denied') setError('Browser push is blocked. Enable it in site settings to subscribe.');
          else if (newState === 'unsupported') setError('This browser doesn\'t support push notifications.');
          return;
        }
        setPrefs({ ...prefs, push: true });
        await notificationService.updatePreferences({ push: true });
      } else {
        const newState = await disablePush();
        setPushState(newState);
        setPrefs({ ...prefs, push: false });
        await notificationService.updatePreferences({ push: false });
      }
    } catch {
      setError('Failed to update push subscription.');
    } finally {
      setSaving(null);
    }
  };

  const saveQuiet = async (patch: Partial<Pick<NotificationPreferenceDTO, 'quietHoursEnabled' | 'quietHoursStart' | 'quietHoursEnd'>>) => {
    if (!prefs) return;
    const prev = prefs;
    const next = { ...prefs, ...patch };
    setPrefs(next);
    setSaving('quiet');
    try {
      await notificationService.updatePreferences(patch);
    } catch {
      setPrefs(prev);
      setError('Failed to save quiet hours.');
    } finally {
      setSaving(null);
    }
  };

  const statusMsg = prefs ? pushStatusText(pushState, prefs.push) : null;
  const pushDisabled = pushState === 'unsupported' || pushState === 'denied';
  const quietSaving = saving === 'quiet';

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
            const isPush = ch.key === 'push';
            const on = ch.lockedOn ? true : prefs[ch.key as 'email' | 'push'];
            const isSaving = saving === ch.key;
            const disabled = ch.lockedOn || isSaving || (isPush && pushDisabled);
            const onChange = () => {
              if (ch.lockedOn || isSaving) return;
              if (ch.key === 'email') void toggleEmail();
              else if (ch.key === 'push') void togglePush();
            };
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
                  {isPush && statusMsg && (
                    <p className="text-[11px] text-amber-600 mt-1 font-medium">{statusMsg}</p>
                  )}
                </div>
                <label
                  className={`shrink-0 inline-flex items-center gap-2 ${
                    disabled ? 'cursor-not-allowed' : 'cursor-pointer'
                  } ${isSaving ? 'opacity-60' : ''}`}
                >
                  <input
                    type="checkbox"
                    checked={on}
                    disabled={disabled}
                    onChange={onChange}
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

      {!loading && prefs && (
        <div className="mt-6 pt-5 border-t border-gray-100">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <span className="text-sm font-semibold text-campus-900">Quiet hours</span>
              <p className="text-xs text-gray-500 mt-0.5">
                Pause email & push during a daily window — they'll be delivered once quiet hours end. In-app still flows through so nothing is missed. Applies to scheduled announcements too.
              </p>
            </div>
            <label className={`shrink-0 inline-flex items-center gap-2 ${quietSaving ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}>
              <input
                type="checkbox"
                checked={prefs.quietHoursEnabled}
                disabled={quietSaving}
                onChange={(e) => void saveQuiet({ quietHoursEnabled: e.target.checked })}
                className="w-5 h-5 rounded border-gray-300 text-campus-600 focus:ring-campus-500 focus:ring-offset-0 cursor-pointer disabled:cursor-not-allowed"
              />
              <span className={`text-xs font-semibold ${prefs.quietHoursEnabled ? 'text-campus-800' : 'text-gray-400'}`}>
                {prefs.quietHoursEnabled ? 'On' : 'Off'}
              </span>
            </label>
          </div>

          {prefs.quietHoursEnabled && (() => {
            const timesDirty =
              draftStart !== prefs.quietHoursStart || draftEnd !== prefs.quietHoursEnd;
            const timesValid = !!draftStart && !!draftEnd && draftStart !== draftEnd;
            const onSaveTimes = async () => {
              if (!timesDirty || !timesValid) return;
              await saveQuiet({ quietHoursStart: draftStart, quietHoursEnd: draftEnd });
            };
            return (
              <div className="mt-3 flex items-end gap-3">
                <label className="flex-1">
                  <span className="block text-[11px] font-semibold uppercase tracking-wider text-gray-500 mb-1">From</span>
                  <input
                    type="time"
                    value={draftStart}
                    disabled={quietSaving}
                    onChange={(e) => setDraftStart(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 focus:border-campus-500 focus:ring-1 focus:ring-campus-500 outline-none"
                  />
                </label>
                <label className="flex-1">
                  <span className="block text-[11px] font-semibold uppercase tracking-wider text-gray-500 mb-1">Until</span>
                  <input
                    type="time"
                    value={draftEnd}
                    disabled={quietSaving}
                    onChange={(e) => setDraftEnd(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 focus:border-campus-500 focus:ring-1 focus:ring-campus-500 outline-none"
                  />
                </label>
                <button
                  type="button"
                  onClick={() => void onSaveTimes()}
                  disabled={!timesDirty || !timesValid || quietSaving}
                  className="h-[38px] px-4 text-xs font-semibold rounded-lg bg-campus-600 text-white hover:bg-campus-700 disabled:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed transition-colors"
                >
                  {quietSaving ? 'Saving…' : 'Save'}
                </button>
              </div>
            );
          })()}
        </div>
      )}
    </div>
  );
}
