package com.document.immigrantvault;

import com.document.immigrantvault.data.db.FolderTree;
import com.document.immigrantvault.data.db.entity.VaultFolder;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FolderTreeTest {

    @Test
    public void childrenOf_returnsOnlyDirectChildren() {
        VaultFolder root = folder(1, null, "Visas");
        VaultFolder child = folder(2, 1L, "H-1B");
        VaultFolder nested = folder(3, 2L, "Receipts");
        List<VaultFolder> all = Arrays.asList(root, child, nested);

        assertEquals(Collections.singletonList(root), FolderTree.childrenOf(all, null));
        assertEquals(Collections.singletonList(child), FolderTree.childrenOf(all, 1L));
        assertEquals(Collections.singletonList(nested), FolderTree.childrenOf(all, 2L));
    }

    @Test
    public void path_joinsAncestorNames() {
        VaultFolder root = folder(1, null, "Visas");
        VaultFolder child = folder(2, 1L, "H-1B");
        List<VaultFolder> all = Arrays.asList(root, child);

        assertEquals("Visas", FolderTree.path(root, all));
        assertEquals("Visas / H-1B", FolderTree.path(child, all));
    }

    @Test
    public void parentsFirst_putsEnclosingFolderAheadOfChild() {
        VaultFolder parent = folder(4, null, "Visas");
        VaultFolder child = folder(9, 4L, "H-1B");
        List<VaultFolder> ordered = FolderTree.parentsFirst(Arrays.asList(child, parent));

        assertEquals(4, ordered.get(0).id);
        assertEquals(9, ordered.get(1).id);
    }

    @Test
    public void inclusiveFileCount_sumsNestedFolders() {
        VaultFolder root = folder(1, null, "Visas");
        VaultFolder child = folder(2, 1L, "H-1B");
        Map<Long, Integer> direct = new HashMap<>();
        direct.put(1L, 1);
        direct.put(2L, 3);

        assertEquals(4, FolderTree.inclusiveFileCount(1, Arrays.asList(root, child), direct));
        assertEquals(3, FolderTree.inclusiveFileCount(2, Arrays.asList(root, child), direct));
    }

    @Test
    public void sanitizeParents_clearsMissingParent() {
        VaultFolder orphan = folder(2, 99L, "Lost");
        FolderTree.sanitizeParents(Collections.singletonList(orphan));
        assertNull(orphan.parentFolderId);
    }

    private static VaultFolder folder(long id, Long parentId, String name) {
        VaultFolder folder = new VaultFolder();
        folder.id = id;
        folder.parentFolderId = parentId;
        folder.name = name;
        return folder;
    }
}
