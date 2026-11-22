import { Star } from 'lucide-react'

// Read-only when no onChange is given; clickable otherwise.
export default function Stars({ value = 0, onChange, size = 16 }) {
  return (
    <span className="stars">
      {[1, 2, 3, 4, 5].map((n) => {
        const icon = (
          <Star
            size={size}
            fill={n <= value ? 'hsl(36 80% 55%)' : 'none'}
            color={n <= value ? 'hsl(36 80% 55%)' : 'hsl(150 10% 70%)'}
          />
        )
        return onChange ? (
          <button type="button" key={n} className="star-btn" onClick={() => onChange(n)}>{icon}</button>
        ) : (
          <span key={n}>{icon}</span>
        )
      })}
    </span>
  )
}
