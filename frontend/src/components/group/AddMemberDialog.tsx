import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { AutoComplete } from 'primereact/autocomplete';
import { useTranslation } from 'react-i18next';
import { userApi } from '../../api/userApi'; // Assuming a general user search endpoint exists
import { useAddMember } from '../../hooks/useGroups';

interface AddMemberDialogProps {
  groupId: string;
  visible: boolean;
  onHide: () => void;
}

const AddMemberDialog = ({ groupId, visible, onHide }: AddMemberDialogProps) => {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [selectedUser, setSelectedUser] = useState<any | null>(null);
  const addMemberMutation = useAddMember(groupId);

  const searchUsers = async (event) => {
    // This endpoint is not in the spec, assuming it exists for the dialog
    // GET /api/v1/users?search=...
    // const results = await userApi.searchUsers(event.query);
    // setSuggestions(results);
    // Mocking for now
    setSuggestions([
        { id: 'mock-user-1', username: 'testuser1' },
        { id: 'mock-user-2', username: 'testuser2' },
    ]);
  };

  const handleAdd = () => {
    if (selectedUser) {
      addMemberMutation.mutate(selectedUser.id, {
        onSuccess: () => {
          onHide();
          setSelectedUser(null);
          setQuery('');
        },
      });
    }
  };

  return (
    <Dialog header={t('groups.addMember')} visible={visible} onHide={onHide} modal>
      <div className="p-fluid">
        <AutoComplete
          value={query}
          suggestions={suggestions}
          completeMethod={searchUsers}
          field="username"
          onChange={(e) => setQuery(e.value)}
          onSelect={(e) => setSelectedUser(e.value)}
          placeholder={t('groups.searchUser')}
        />
      </div>
      <div className="p-dialog-footer mt-4">
        <Button label={t('common.cancel')} icon="pi pi-times" onClick={onHide} className="p-button-text" />
        <Button label={t('common.add')} icon="pi pi-check" onClick={handleAdd} disabled={!selectedUser || addMemberMutation.isLoading} />
      </div>
    </Dialog>
  );
};

export default AddMemberDialog;
