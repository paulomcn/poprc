import { AlertCircle, CheckCircle, Info } from 'lucide-react'

export default function Alert({ type = 'info', message, onClose }) {
  const bgColor = {
    success: 'bg-green-50 border-green-200',
    error: 'bg-red-50 border-red-200',
    info: 'bg-blue-50 border-blue-200',
  }[type]

  const icon = {
    success: <CheckCircle className="text-green-600" size={20} />,
    error: <AlertCircle className="text-red-600" size={20} />,
    info: <Info className="text-blue-600" size={20} />,
  }[type]

  const textColor = {
    success: 'text-green-800',
    error: 'text-red-800',
    info: 'text-blue-800',
  }[type]

  return (
    <div className={`border rounded-lg p-4 flex items-gap-3 ${bgColor}`}>
      <div className="flex-shrink-0">{icon}</div>
      <div className="flex-1 ml-3">
        <p className={`text-sm ${textColor}`}>{message}</p>
      </div>
      {onClose && (
        <button
          onClick={onClose}
          className="ml-4 text-gray-400 hover:text-gray-600"
        >
          ×
        </button>
      )}
    </div>
  )
}
