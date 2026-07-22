export default function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}) {
  return (
    <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-end lg:justify-between">
      <div className="min-w-0">
        {eyebrow && (
          <p className="text-xs font-bold uppercase text-blue-700">{eyebrow}</p>
        )}
        <h1 className="mt-1 text-2xl font-bold text-slate-950 sm:text-3xl">{title}</h1>
        {description && (
          <p className="mt-1 max-w-3xl text-sm leading-6 text-slate-500">{description}</p>
        )}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </header>
  );
}
