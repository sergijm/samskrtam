import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { AutoComplete } from 'primereact/autocomplete';
import { useTranslation } from 'react-i18next';
import { userApi } from '../../api/userApi';
import { useAddMember } from '../../hooks/useGroups';
import { User } from '../../types/user'; // Added import for User type

interface AddMemberDialogProps {
  groupId: string;
  visible: boolean;
  onHide: () => void;
}

const AddMemberDialog = ({ groupId, visible, onHide }: AddMemberDialogProps) => {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<User[]>([]); // Changed type to User[]
  const [selectedUser, setSelectedUser] = useState<User | null>(null); // Changed type to User
  const addMemberMutation = useAddMember(groupId);

  const searchUsers = async (event: { query: string }) => {
    try {
      const response = await userApi.searchUsers(event.query);
      setSuggestions(response.data);
    } catch (error) {
      console.error('Error searching users:', error);
      setSuggestions([]);
    }
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
          value={selectedUser ? selectedUser.username : query} // Display selected username or current query
          suggestions={suggestions}
          completeMethod={searchUsers}
          field="username"
          onChange={(e) => setQuery(e.value)}
          onSelect={(e) => setSelectedUser(e.value)}
          placeholder={t('groups.searchUser')}
          itemTemplate={(item: User) => <div>{item.username} ({item.email})</div>} // Custom template for suggestions
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
