"path = 'services/sangraha-service/src/main/java/sm/selflearn/samskrtam/sangraha/service/WorkService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove updateWork(UUID) — from '@Transactional' to just before 'updateWorkBySlug'
old1 = \"\"\"    @Transactional
    public Work updateWork(UUID id, Work update) {
        Work work = getWorkById(id);
        work.setTitleRu(update.getTitleRu());
        work.setTitleEn(update.getTitleEn());
        work.setTitleSaIast(update.getTitleSaIast());
        work.setTitleSaDevanagari(update.getTitleSaDevanagari());
        work.setDescriptionRu(update.getDescriptionRu());
        work.setDescriptionEn(update.getDescriptionEn());
        work.setAuthor(update.getAuthor());
        return workRepository.save(work);
    }

    @Transactional
    public Work updateWorkBySlug\"\"\"

new1 = \"    @Transactional\\n    public Work updateWorkBySlug\"

content = content.replace(old1, new1)

# Remove deleteWork(UUID)
old2 = \"\"\"    @Transactional
    public void deleteWork(UUID id) {
        Work work = getWorkById(id);
        work.setDeletedAt(Instant.now());
        workRepository.save(work);
    }

    @Transactional
    public void deleteWorkBySlug\"\"\"

new2 = \"    @Transactional\\n    public void deleteWorkBySlug\"

content = content.replace(old2, new2)

# Update Javadoc for getWorkByIdOrSlug
old3 = \"\"\"    /**
     * Get a work by UUID string or slug. Tries UUID parsing first,
     * falls back to slug lookup.
     */\"\"\"

new3 = \"\"\"    /**
     * Internal-only. Get a work by UUID string or slug.
     * Tries UUID parsing first, falls back to slug lookup.
     * Not bound to any REST endpoint.
     */\"\"\"

content = content.replace(old3, new3)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done')
"