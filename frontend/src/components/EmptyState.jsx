import { Inbox } from "lucide-react";

export default function EmptyState({
  icon: Icon = Inbox,
  title,
  description,
  action,
  compact = false,
}) {
  return (
    <div className={`flex flex-col items-center justify-center px-5 text-center ${compact ? "py-8" : "py-14"}`}>
      <span className="flex h-11 w-11 items-center justify-center rounded border border-slate-200 bg-slate-50 text-slate-400">
        <Icon size={21} aria-hidden="true" />
      </span>
      <h3 className="mt-3 text-sm font-bold text-slate-800">{title}</h3>
      {description && <p className="mt-1 max-w-md text-xs leading-5 text-slate-500">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
