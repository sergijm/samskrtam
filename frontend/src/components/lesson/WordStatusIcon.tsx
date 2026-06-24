import React from 'react';

interface WordStatusIconProps {
  status: 'NEW'|'LEARNING'|'REVIEW'|'MASTERED'
}

export const WordStatusIcon = ({ status }: WordStatusIconProps) => {
  switch (status) {
    case 'NOT_STARTED':
      return <i className="pi pi-circle text-color-secondary"></i>;
    case 'IN_PROGRESS':
      return <i className="pi pi-spin pi-spinner text-primary"></i>;
    case 'LEARNED':
      return <i className="pi pi-check-circle text-green-500"></i>;
    default:
      return <i className="pi pi-circle text-color-secondary"></i>;
  }
};