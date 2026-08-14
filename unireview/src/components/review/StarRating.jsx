import { useState } from 'react';
import { FiStar } from 'react-icons/fi';
import { FaStar } from 'react-icons/fa';
import './StarRating.css';

export default function StarRating({ value = 0, onChange, size = 24, readonly = false, label }) {
  const [hover, setHover] = useState(0);

  return (
    <div className="star-rating-wrapper">
      {label && <span className="star-rating-label">{label}</span>}
      <div className="star-rating" role="radiogroup" aria-label={label || 'Rating'}>
        {[1, 2, 3, 4, 5].map((star) => {
          const isFilled = star <= (hover || value);
          return (
            <button
              key={star}
              type="button"
              className={`star-btn ${isFilled ? 'filled' : ''} ${readonly ? 'readonly' : ''}`}
              style={{ fontSize: size }}
              onClick={() => !readonly && onChange?.(star)}
              onMouseEnter={() => !readonly && setHover(star)}
              onMouseLeave={() => !readonly && setHover(0)}
              aria-label={`${star} sao`}
              disabled={readonly}
            >
              {isFilled ? <FaStar /> : <FiStar />}
            </button>
          );
        })}
        {value > 0 && <span className="star-value">{value}/5</span>}
      </div>
    </div>
  );
}
