import { useEffect, useMemo, useState, type FormEvent } from 'react';
import subscribeHeroImage from '../../../assets/newsletter-subscribe-2026.svg';
import { registerIdolGlowSubscription } from '../../../shared/data/subscriptionApi';
import styles from './NewsletterSubscribeModal.module.css';

interface NewsletterSubscribeModalProps {
  readonly open: boolean;
  readonly onClose: () => void;
}

const isValidEmail = (email: string): boolean => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

const CloseIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <path d="M18 6L6 18M6 6l12 12" />
  </svg>
);

export const NewsletterSubscribeModal = ({
  open,
  onClose,
}: NewsletterSubscribeModalProps) => {
  const [email, setEmail] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successOpen, setSuccessOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }

    document.documentElement.style.overflow = 'hidden';

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !submitting) {
        onClose();
      }
    };

    document.addEventListener('keydown', onKeyDown);

    return () => {
      document.documentElement.style.overflow = '';
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [onClose, open, submitting]);

  useEffect(() => {
    if (!open) {
      setEmail('');
      setAgreed(false);
      setError(null);
      setSuccessOpen(false);
      setSubmitting(false);
    }
  }, [open]);

  const helperMessage = useMemo(() => {
    if (error) {
      return error;
    }
    return 'IdolGlow 홈페이지 회원가입을 통해서도 Idol Glow 소식과 호별보기를 받아보실 수 있습니다.';
  }, [error]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!email.trim()) {
      setError('이메일 주소를 입력해 주세요.');
      return;
    }

    if (!isValidEmail(email.trim())) {
      setError('올바른 이메일 형식으로 입력해 주세요.');
      return;
    }

    if (!agreed) {
      setError('개인정보 수집 및 활용 동의에 체크해 주세요.');
      return;
    }

    setError(null);
    setSubmitting(true);

    try {
      await registerIdolGlowSubscription({
        email: email.trim(),
        agreedToPrivacy: true,
        subscribeNewsletters: true,
        subscribeIssues: true,
      });
      setSuccessOpen(true);
    } catch (submitError) {
      setError(
        submitError instanceof Error ? submitError.message : '구독 신청 처리에 실패했습니다.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    if (submitting) {
      return;
    }
    if (successOpen) {
      setSuccessOpen(false);
    }
    onClose();
  };

  const handleConfirmSuccess = () => {
    setSuccessOpen(false);
    onClose();
  };

  if (!open) {
    return null;
  }

  return (
    <div className={styles.backdrop} role="presentation" onClick={handleClose}>
      <div
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="newsletter-subscribe-title"
        onClick={event => event.stopPropagation()}
      >
        <button
          type="button"
          className={styles.closeButton}
          onClick={handleClose}
          aria-label="구독 신청 창 닫기"
          disabled={submitting}
        >
          <CloseIcon />
        </button>

        <div className={styles.leftPanel}>
          <span className={styles.kicker}>IDOLGLOW NEWSLETTER</span>
          <h2 id="newsletter-subscribe-title" className={styles.title}>
            지금 Idol Glow를
            <br />
            구독해보세요!
          </h2>

          <div className={styles.heroImageFrame}>
            <img
              src={subscribeHeroImage}
              alt="2026 Idol Glow 뉴스레터 대표 이미지"
              className={styles.heroImage}
            />
          </div>

          <p className={styles.leftCaption}>
            문화예술 소식과 웹진 업데이트를 메일로 빠르게 받아보고, 새로 발행된 소식지와
            호별보기를 놓치지 마세요.
          </p>
        </div>

        <div className={styles.rightPanel}>
          <form className={styles.form} onSubmit={handleSubmit} noValidate>
            <div className={styles.sectionHeader}>
              <h3 className={styles.sectionTitle}>구독 신청</h3>
              <p className={styles.sectionDescription}>
                이메일과 개인정보 수집 동의 후 Idol Glow 메일 구독을 신청할 수 있습니다.
              </p>
            </div>

            <div className={styles.fieldBlock}>
              <label htmlFor="newsletter-subscribe-email" className={styles.srOnly}>
                이메일 주소 입력
              </label>
              <input
                id="newsletter-subscribe-email"
                type="email"
                className={styles.input}
                placeholder="이메일 주소 입력"
                value={email}
                disabled={submitting}
                onChange={event => {
                  setEmail(event.target.value);
                  if (error) {
                    setError(null);
                  }
                }}
              />
              <p className={[styles.helperText, error ? styles.helperError : ''].filter(Boolean).join(' ')}>
                {helperMessage}
              </p>
            </div>

            <div className={styles.agreementCard}>
              <h4 className={styles.agreementTitle}>개인정보 수집 및 활용 동의</h4>
              <dl className={styles.agreementList}>
                <div>
                  <dt>1. 이용목적</dt>
                  <dd>'Idol Glow' 메일 발송</dd>
                </div>
                <div>
                  <dt>2. 수집항목</dt>
                  <dd>이메일</dd>
                </div>
                <div>
                  <dt>3. 보유기간</dt>
                  <dd>'구독 취소' 시 이메일 정보는 삭제됩니다.</dd>
                </div>
                <div>
                  <dt>4. 동의여부</dt>
                  <dd>
                    개인정보 수집 동의 후 Idol Glow 메일을 받아보실 수 있습니다.
                    <br />
                    메일 발송 목적 이외의 다른 목적으로 사용하지 않습니다.
                  </dd>
                </div>
              </dl>

              <label className={styles.agreeRow}>
                <input
                  type="checkbox"
                  checked={agreed}
                  disabled={submitting}
                  onChange={event => {
                    setAgreed(event.target.checked);
                    if (error) {
                      setError(null);
                    }
                  }}
                />
                <span>개인정보 수집 및 활용에 동의합니다.</span>
              </label>
            </div>

            <div className={styles.buttonRow}>
              <button type="submit" className={styles.submitButton} disabled={submitting}>
                {submitting ? '신청 중...' : '구독신청'}
              </button>
              <button
                type="button"
                className={styles.cancelButton}
                onClick={handleClose}
                disabled={submitting}
              >
                구독취소
              </button>
            </div>
          </form>
        </div>

        {successOpen ? (
          <div className={styles.successBackdrop} role="presentation">
            <div className={styles.successDialog} role="alertdialog" aria-modal="true">
              <h3 className={styles.successTitle}>구독신청되었습니다.</h3>
              <p className={styles.successDescription}>
                확인 버튼을 누르면 구독 입력 창이 사라집니다.
              </p>
              <button type="button" className={styles.successButton} onClick={handleConfirmSuccess}>
                확인
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default NewsletterSubscribeModal;
