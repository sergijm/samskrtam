import React from 'react';

interface WordStatusIconProps {
  status: 'NEW'|'LEARNING'|'REVIEW'|'MASTERED'
}

export const WordStatusIcon = ({ status }: WordStatusIconProps) => {
  switch (status) {
    case 'NEW':
      return <i className="pi pi-circle text-color-secondary"></i>;
    case 'LEARNING':
      return <i className="pi pi-spin pi-spinner text-primary"></i>;
    case 'REVIEW':
      return <i className="pi pi-exclamation-circle text-yellow-500"></i>;
    case 'MASTERED':
      return <i className="pi pi-check-circle text-green-500"></i>;
    default:
      return <i className="pi pi-circle text-color-secondary"></i>;
  }
};