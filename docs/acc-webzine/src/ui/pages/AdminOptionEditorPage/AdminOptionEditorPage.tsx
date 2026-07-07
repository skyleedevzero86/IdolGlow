import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthContext';
import MarkdownEditorField from '../../components/MarkdownEditorField/MarkdownEditorField';
import {
  createAdminOption,
  fetchAdminOption,
  updateAdminOption,
} from '../../../shared/data/adminBookingApi';
import { AdminMarketingShell } from '../AdminMarketingPage/AdminMarketingShell';
import shellStyles from '../AdminMarketingPage/AdminMarketingPage.module.css';
import shellToolbar from '../AdminUsersPage/AdminUsersShellToolbar.module.css';
import styles from '../AdminBookingManagement/AdminBookingManagement.module.css';

type OptionFormState = {
  readonly name: string;
  readonly description: string;
  readonly price: string;
  readonly location: string;
};

const EMPTY_FORM: OptionFormState = {
  name: '',
  description: '',
  price: '',
  location: '',
};

export function AdminOptionEditorPage() {
  const { optionId } = useParams<{ optionId: string }>();
  const isEdit = Boolean(optionId);
  const navigate = useNavigate();
  const { accessToken, authReady, user } = useAuth();
  const [form, setForm] = useState<OptionFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authReady || user?.role !== 'ADMIN' || !optionId) {
      return;
    }

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const detail = await fetchAdminOption(accessToken, Number(optionId));
        setForm({
          name: detail.name,
          description: detail.description,
          price: String(detail.price),
          location: detail.location,
        });
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : '옵션 정보를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [accessToken, authReady, optionId, user?.role]);

  const handleSubmit = async () => {
    const price = Number(form.price);
    if (Number.isNaN(price) || price < 0) {
      setError('옵션명, 설명, 금액(0 이상), 장소를 올바르게 입력해 주세요.');
      return;
    }
    if (!form.name.trim() || !form.description.trim() || !form.location.trim()) {
      setError('옵션명, 설명, 금액, 장소를 입력해 주세요.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: form.name,
        description: form.description,
        price,
        location: form.location,
      };

      if (optionId) {
        await updateAdminOption(accessToken, Number(optionId), payload);
      } else {
        await createAdminOption(accessToken, payload);
      }

      navigate('/admin/options');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : '옵션을 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  };

  if (!authReady) {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.loading}>관리자 권한을 확인하는 중입니다.</div>
      </main>
    );
  }

  if (user?.role !== 'ADMIN') {
    return (
      <main className={shellStyles.page}>
        <div className={shellStyles.denied}>관리자만 옵션 편집 화면을 사용할 수 있습니다.</div>
      </main>
    );
  }

  return (
    <AdminMarketingShell
      currentPath="/admin/options"
      title={isEdit ? '옵션 수정' : '옵션 등록'}
      description=""
      classNames={{
        toolbarCard: shellToolbar.toolbarCard,
        header: shellToolbar.header,
        title: shellToolbar.title,
        statusText: shellToolbar.statusText,
      }}
      statusText={error ?? (loading ? '옵션 정보를 불러오는 중입니다.' : null)}
      stats={[]}
    >
      <section className={styles.section}>
        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span className={styles.label}>옵션명</span>
                <input
                  className={styles.input}
                  value={form.name}
                  onChange={event => setForm(previous => ({ ...previous, name: event.target.value }))}
                />
              </label>

              <label className={styles.field}>
                <span className={styles.label}>장소</span>
                <input
                  className={styles.input}
                  value={form.location}
                  onChange={event =>
                    setForm(previous => ({ ...previous, location: event.target.value }))
                  }
                />
              </label>

              <label className={`${styles.field} ${styles.fullWidth}`}>
                <span className={styles.label}>옵션 추가 금액 (0~)</span>
                <input
                  className={styles.input}
                  type="number"
                  min={0}
                  step="1"
                  value={form.price}
                  onChange={event => setForm(previous => ({ ...previous, price: event.target.value }))}
                  placeholder="0 이상 (원)"
                />
              </label>
            </div>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <MarkdownEditorField
              label="옵션 설명"
              value={form.description}
              onChange={value => setForm(previous => ({ ...previous, description: value }))}
              placeholder="옵션 설명을 마크다운으로 작성해 주세요."
            />
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelBody}>
            <div className={styles.buttonRow}>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void handleSubmit()}
                disabled={saving || loading}
              >
                {saving ? '저장 중...' : isEdit ? '수정 저장' : '등록 저장'}
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => navigate('/admin/options')}
              >
                목록 이동
              </button>
            </div>
          </div>
        </section>
      </section>
    </AdminMarketingShell>
  );
}

export default AdminOptionEditorPage;
