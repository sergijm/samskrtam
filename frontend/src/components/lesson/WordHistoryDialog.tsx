import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { useWordHistory } from '../../hooks/useLessons';
import { Skeleton } from 'primereact/skeleton';

interface WordHistoryDialogProps {
  visible: boolean;
  onHide: () => void;
  wordId: string | null;
  quizId: string;
}

export const WordHistoryDialog = ({ 
  visible, 
  onHide, 
  wordId, 
  quizId 
}: WordHistoryDialogProps) => {
  const { data: history, isLoading } = useWordHistory(quizId, wordId || '');
  
  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU');
  };
  
  const getAnswerStatus = (isCorrect: boolean) => {
    return (
      <div className="flex align-items-center">
        {isCorrect ? (
          <i className="pi pi-check text-green-500"></i>
        ) : (
          <i className="pi pi-times text-red-500"></i>
        )}
        <span className="ml-2">{isCorrect ? 'Правильно' : 'Неправильно'}</span>
      </div>
    );
  };
  
  const header = wordId && history ? `История ответов: ${history.word}` : 'История ответов';
  
  return (
    <Dialog 
      visible={visible} 
      onHide={onHide} 
      header={header}
      style={{ width: '80vw' }}
      maximizable
    >
      {isLoading || !history ? (
        <div className="p-4">
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
        </div>
      ) : (
        <div>
          <DataTable 
            value={history.entries}
            paginator 
            rows={10}
            responsiveLayout="scroll"
          >
            <Column 
              header="Дата" 
              body={(rowData) => formatDateTime(rowData.answeredAt)} 
              style={{ width: '20%' }}
            />
            <Column 
              header="Правильный ответ" 
              body={(rowData) => rowData.correctAnswer} 
              style={{ width: '30%' }}
            />
            <Column 
              header="Ответ пользователя" 
              body={(rowData) => rowData.userAnswer} 
              style={{ width: '30%' }}
            />
            <Column 
              header="Статус" 
              body={(rowData) => getAnswerStatus(rowData.isCorrect)} 
              style={{ width: '20%' }}
            />
          </DataTable>
        </div>
      )}
    </Dialog>
  );
};