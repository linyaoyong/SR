interface PlaceholderPageProps {
  title?: string;
}

export default function PlaceholderPage({ title = '页面待实现' }: PlaceholderPageProps) {
  return (
    <main className="sr-page">
      <section className="sr-section">
        <div className="sr-card">
          <p className="sr-caption">邻享租借平台</p>
          <h1 className="sr-title">{title}</h1>
          <p className="sr-text">该页面将在后续任务中实现。</p>
        </div>
      </section>
    </main>
  );
}
