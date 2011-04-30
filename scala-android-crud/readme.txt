Three major components: View, Persistence, Model (for business logic).
They all share the same Entities with Fields but have their own PortableFields.
View has UIContext (access to system UI environment and parent window),
      View instances, resource string and view ids for CRUD UI
UIContext is NOT tied to an Entity, so it can support CRUD and UI on ANY Entity.

Actions:
create - creates Entry View in UIContext, copies from Unit to View, allows user input, then View to Table format, and save (insert), closes Entry View
    startCreate: EntryView, create(data): id, close(EntryView)
read/list - [optional query: creates Query View in UIContext, possibly allows user query input, copies from View to Query]
            (or copies from model to simple Query), find/findAll, create List View, then copy Table format to View, user can close View
    startCriteria(criteria): CriteriaView, query(criteria): data stream, close(CriteriaView)
    startList(criteria): ListView, close(ListView), startRead(id): EntityView, read(id): Option[data], close(EntityView)
update - (after read/list), create Entry View, copies from Table format to View, allows user input,
             copies from View to Table format, save (update), close Entry View
    startUpdate(id): EntryView, save(id, data), close(EntryView)
delete - (after read/list) copies from View to simple value Query, optional prompt, delete, optional support for undo
    startDelete(id or criteria): DeleteView, delete(id or data stream), close(DeleteView)


ToDos:
* Make EntityPersistence.find(ID) return an Option[R] instead of just R.