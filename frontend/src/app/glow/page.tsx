import Link from "next/link";
import styles from "./glow.module.css";

const articles = [
  {
    category: "공연",
    title: "2026 차세대 아시아 예술인 커뮤니티",
    image: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 108",
    tags: "#국제교류 #아시아문화",
    likes: 12,
    views: 190,
  },
  {
    category: "전시",
    title: "우리는 기술시대에 어떻게 응답할 것인가",
    image: "https://images.unsplash.com/photo-1564399579883-451a5d44ec08?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 108",
    tags: "#ACT페스티벌 #미디어아트",
    likes: 21,
    views: 359,
  },
  {
    category: "전시",
    title: "빛이 머문 자리, 찬란한 순간을 걷다",
    image: "https://images.unsplash.com/photo-1561214115-f2f134cc4912?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 108",
    tags: "#신진작가 #현대미술",
    likes: 17,
    views: 548,
  },
  {
    category: "행사·교류",
    title: "〈2026 책 읽는 ACC〉 시(書)로 보고, 잇고, 나누다",
    image: "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 108",
    tags: "#책읽는ACC #책의얼굴들",
    likes: 20,
    views: 174,
  },
  {
    category: "행사·교류",
    title: "아람문화제가 보여준 아람의 여러 얼굴",
    image: "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 108",
    tags: "#아람문화제 #아리랑",
    likes: 22,
    views: 146,
  },
  {
    category: "전시",
    title: "2026 ACC 미래상: 감염은 이동하는 소리",
    image: "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 107",
    tags: "#미래상 #김영은작가",
    likes: 13,
    views: 686,
  },
  {
    category: "행사·교류",
    title: "ACC 국제 콜로퀴엄, 지향하며 생성성으로",
    image: "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 107",
    tags: "#국제콜로퀴엄 #아시아",
    likes: 8,
    views: 266,
  },
  {
    category: "전시",
    title: "삶을 짓는 그림, 내일을 향한 마음들",
    image: "https://images.unsplash.com/photo-1541961017774-22349e4a1262?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 107",
    tags: "#예술 #마음",
    likes: 18,
    views: 277,
  },
  {
    category: "교육·포럼",
    title: "아시아 예술체험, 손끝에서 만나는 공예",
    image: "https://images.unsplash.com/photo-1452860606245-08befc0ff44b?auto=format&fit=crop&w=900&q=85",
    volume: "Vol. 107",
    tags: "#예술체험 #공예",
    likes: 19,
    views: 374,
  },
];

const popularArticles = [
  {
    category: "행사·교류",
    title: "시니어 세대를 위한 연극형 전시 관람 투어",
    image: "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=700&q=85",
    volume: "Vol. 106",
    views: 729,
  },
  {
    category: "전시",
    title: "낯선 세계에서 발견한 삶의 이야기",
    image: "https://images.unsplash.com/photo-1577083552431-6e5fd01aa342?auto=format&fit=crop&w=700&q=85",
    volume: "Vol. 105",
    views: 604,
  },
  {
    category: "전시",
    title: "새로운 퍼즐이 되어 엮어내는 아시아의 미래 서사",
    image: "https://images.unsplash.com/photo-1549490349-8643362247b5?auto=format&fit=crop&w=700&q=85",
    volume: "Vol. 104",
    views: 643,
  },
];

function ArticleCard({ article }: { article: (typeof articles)[number] }) {
  return (
    <article className={styles.card}>
      <div className={styles.cardImageWrap}>
        <img className={styles.cardImage} src={article.image} alt="" />
      </div>
      <div className={styles.cardBody}>
        <div className={styles.cardMeta}>
          <span className={styles.badge}>{article.category}</span>
          <span>♡ {article.likes}</span>
          <span>◉ {article.views}</span>
        </div>
        <h2>{article.title}</h2>
        <div className={styles.cardFooter}>
          <strong>{article.volume}</strong>
          <span>{article.tags}</span>
        </div>
      </div>
    </article>
  );
}

export default function GlowPage() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link href="/" className={styles.logo}>IDOL <span>GLOW</span></Link>
          <nav className={styles.nav} aria-label="주요 메뉴">
            <Link className={styles.active} href="/glow">GLOW</Link>
            <Link href="/wish">WISH</Link>
            <Link href="/events">이벤트</Link>
            <Link href="/archive">아카이브</Link>
          </nav>
          <div className={styles.actions} aria-label="사용자 메뉴">
            <button type="button">로그인</button>
            <span>◎ KO</span><span>⌕</span><span>☾</span><span>♧</span>
          </div>
        </div>
      </header>

      <main>
        <section className={styles.hero} aria-label="추천 콘텐츠">
          <img
            className={styles.heroImage}
            src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=2000&q=90"
            alt="바위 해안과 푸른 바다"
          />
          <div className={styles.heroShade} />
          <div className={styles.heroContent}>
            <span className={styles.heroLabel}>이벤트</span>
            <h1>&quot;쿠폰 팩&quot;을 검색해보세요</h1>
            <p>특별한 여행과 문화 혜택을 한 번에 만나보세요.</p>
            <div className={styles.heroControls}><button>‹</button><span>1 / 3</span><button>›</button></div>
          </div>
          <div className={styles.heroLinks}>
            <Link href="#contents">조회하기</Link>
            <Link href="#contents">Idol Glow 소식</Link>
            <Link href="#contents">구독 신청</Link>
          </div>
        </section>

        <section className={styles.contentSection} id="contents">
          <div className={styles.contentInner}>
            <div className={styles.primaryColumn}>
              <div className={styles.filters} aria-label="콘텐츠 분류">
                {['전체', '트렌드', 'glow정보', '최근리뷰', '지역사회'].map((filter, index) => (
                  <button className={index === 0 ? styles.filterActive : undefined} key={filter}>{filter}</button>
                ))}
              </div>
              <div className={styles.grid}>
                {articles.map((article) => <ArticleCard key={article.title} article={article} />)}
              </div>
              <button className={styles.moreButton} type="button" aria-label="콘텐츠 더 보기">＋</button>
            </div>

            <aside className={styles.popular} aria-label="많이 본 콘텐츠">
              <div className={styles.popularTabs}><strong>최신 콘텐츠</strong><span>많이 본 콘텐츠</span></div>
              <div className={styles.popularList}>
                {popularArticles.map((article) => (
                  <article className={styles.popularCard} key={article.title}>
                    <img src={article.image} alt="" />
                    <div className={styles.popularBody}>
                      <div><span className={styles.popularBadge}>{article.category}</span><span>✦</span><span>◉ {article.views}</span></div>
                      <h2>{article.title}</h2>
                      <p>{article.volume} &nbsp; #IDOLGLOW</p>
                    </div>
                  </article>
                ))}
              </div>
            </aside>
          </div>
        </section>
      </main>

      <aside className={styles.quickMenu} aria-label="빠른 메뉴">
        <strong>최근 본 콘텐츠</strong><span>최근 본 콘텐츠가<br />없습니다.</span>
        <button type="button">Idol Glow<br />소식</button><button type="button">호텔찾기</button>
        <a href="#top" aria-label="맨 위로">↑</a>
      </aside>

      <footer className={styles.footer}>
        <Link href="/" className={styles.footerLogo}>IDOL <span>GLOW</span></Link>
        <div className={styles.socials}><span>◎</span><span>▶</span><span>●</span></div>
        <p>1986-2026</p><small>Copyright 2026. Idol Glow. All rights reserved.</small>
      </footer>
    </div>
  );
}
