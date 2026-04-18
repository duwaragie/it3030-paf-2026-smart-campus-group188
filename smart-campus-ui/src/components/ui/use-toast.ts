import { useCallback } from 'react';

export interface Toast {
  title: string;
  description?: string;
  variant?: 'default' | 'destructive';
}

export function useToast() {
  const toast = useCallback((props: Toast) => {
    const message = props.description ? `${props.title}: ${props.description}` : props.title;
    
    if (props.variant === 'destructive') {
      console.error(message);
    } else {
      console.log(message);
    }
    
    // You can replace this with a proper toast library later
    // For now, we're using console and alert as fallback
    if (typeof window !== 'undefined') {
      if (props.variant === 'destructive') {
        alert(`Error: ${message}`);
      } else {
        alert(`Success: ${message}`);
      }
    }
  }, []);

  return { toast };
}
