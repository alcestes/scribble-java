/*
 * Copyright 2009-12 www.scribble.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.protocol.export.monitor;

import java.util.UUID;
import java.util.logging.Logger;

import org.scribble.common.logging.Journal;
import org.scribble.protocol.model.Activity;
import org.scribble.protocol.model.Block;
import org.scribble.protocol.model.CustomActivity;
import org.scribble.protocol.model.DefaultVisitor;
import org.scribble.protocol.model.DirectedChoice;
import org.scribble.protocol.model.Interaction;
import org.scribble.protocol.model.Interrupt;
import org.scribble.protocol.model.Introduces;
import org.scribble.protocol.model.MessageSignature;
import org.scribble.protocol.model.ModelObject;
import org.scribble.protocol.model.ProtocolImportList;
import org.scribble.protocol.model.RecBlock;
import org.scribble.protocol.model.Recursion;
import org.scribble.protocol.model.Repeat;
import org.scribble.protocol.model.Role;
import org.scribble.protocol.model.TypeImport;
import org.scribble.protocol.model.TypeImportList;
import org.scribble.protocol.model.TypeReference;
import org.scribble.protocol.model.Unordered;
import org.scribble.protocol.monitor.model.Call;
import org.scribble.protocol.monitor.model.Decision;
import org.scribble.protocol.monitor.model.Description;
import org.scribble.protocol.monitor.model.MessageNode;
import org.scribble.protocol.monitor.model.MessageType;
import org.scribble.protocol.monitor.model.Node;
import org.scribble.protocol.monitor.model.Path;
import org.scribble.protocol.monitor.model.ReceiveMessage;
import org.scribble.protocol.monitor.model.Scope;
import org.scribble.protocol.monitor.model.SendMessage;
import org.scribble.protocol.util.RunUtil;
import org.scribble.protocol.util.TypesUtil;

/**
 * This class provides the visitor for traversing the model to export
 * to the monitor description.
 *
 */
public class MonitorExportVisitor extends DefaultVisitor {
    
	private static Logger LOG=Logger.getLogger(MonitorExportVisitor.class.getName());
	
    private Journal _journal=null;
    
    // This list defines the nodes used to create the final representation of the
    // state machine
    private java.util.List<Node> _nodes=
        new java.util.Vector<Node>();
    
    private java.util.Map<org.scribble.protocol.monitor.model.Choice,java.util.List<Path>> _choicePaths=
        new java.util.HashMap<org.scribble.protocol.monitor.model.Choice,java.util.List<Path>>();
    
    private java.util.Map<org.scribble.protocol.monitor.model.Parallel,java.util.List<Path>> _parallelPaths=
        new java.util.HashMap<org.scribble.protocol.monitor.model.Parallel,java.util.List<Path>>();
    
    private java.util.Map<String,Integer> _recurPosition=
        new java.util.HashMap<String,Integer>();
    
    // This list represents a cache of nodes that are awaiting their 'nextIndex' field
    // to be set
    private java.util.List<Object> _pendingNextIndex=
        new java.util.Vector<Object>();
    
    // This map contains a reference from an activity, to other relevant information required
    // later (e.g. nodes, etc).
    private java.util.Map<ModelObject,Object> _nodeMap=
        new java.util.HashMap<ModelObject,Object>();
    
    private java.util.Map<ModelObject,java.util.List<Object>> _nodeCache=
        new java.util.HashMap<ModelObject,java.util.List<Object>>();
    
    /**
     * This method sets the journal.
     * 
     * @param journal The journal
     */
    public void setJournal(Journal journal) {
        _journal = journal;
    }
    
    /**
     * This method returns the list of nodes.
     * 
     * @return The list of nodes
     */
    protected java.util.List<Node> getNodes() {
    	return(_nodes);
    }
    
    /**
     * This method returns the pending next index list.
     * 
     * @return The pending next index list
     */
    protected java.util.List<Object> getPendingNextIndex() {
    	return (_pendingNextIndex);
    }
    
    /**
     * This method indicates the start of a
     * block.
     * 
     * @param elem The block
     * @return Whether to process the contents
     */
    public boolean start(Block elem) {
        startActivity(elem);
        
        return (true);
    }
    
    /**
     * This method processes the start of an activity.
     * 
     * @param act The activity
     */
    protected void startActivity(Activity act) {
        
        // Check if activity is block and parent is parallel
        if ((act instanceof Block && act.getParent() instanceof org.scribble.protocol.model.Parallel)
                || (act.getParent() instanceof Block
                        && act.getParent().getParent() instanceof Unordered)) {
            
            // Create path node
            Path path=new Path();
            
            org.scribble.protocol.monitor.model.Parallel node=getParallelNode(act);

            java.util.List<Path> pathBuilderList=
                        _parallelPaths.get(node);
            
            if (pathBuilderList == null) {
                pathBuilderList = new java.util.Vector<Path>();
                _parallelPaths.put(node, pathBuilderList);
            }
            
            pathBuilderList.add(path);
            
            _pendingNextIndex.add(path);

            // Create annotations
            for (org.scribble.common.model.Annotation pma : act.getAnnotations()) {
                org.scribble.protocol.monitor.model.Annotation pmma=
                            new org.scribble.protocol.monitor.model.Annotation();
                
                if (pma.getId() != null) {
                    pmma.setId(pma.getId());
                } else {
                    pmma.setId(UUID.randomUUID().toString());
                }
                pmma.setValue(pma.toString());
                
                path.getAnnotation().add(pmma);
            }

        } else if (act instanceof Block && act.getParent() instanceof org.scribble.protocol.model.Choice) {
            
            // Create path node
            Path path=new Path();
            
            // Define id associated with the choice label
            //path.setId(ChoiceUtil.getLabel(elem.getMessageSignature()));
            
            org.scribble.protocol.monitor.model.Choice choiceBuilder=
                (org.scribble.protocol.monitor.model.Choice)_nodeMap.get(act.getParent());

            java.util.List<Path> pathBuilderList=
                        _choicePaths.get(choiceBuilder);
            
            if (pathBuilderList == null) {
                pathBuilderList = new java.util.Vector<Path>();
                _choicePaths.put(choiceBuilder, pathBuilderList);
            }
            
            pathBuilderList.add(path);
            
            _pendingNextIndex.add(path);

            // Create annotations
            for (org.scribble.common.model.Annotation pma : act.getAnnotations()) {
                org.scribble.protocol.monitor.model.Annotation pmma=
                            new org.scribble.protocol.monitor.model.Annotation();
                
                if (pma.getId() != null) {
                    pmma.setId(pma.getId());
                } else {
                    pmma.setId(UUID.randomUUID().toString());
                }
                pmma.setValue(pma.toString());
                
                path.getAnnotation().add(pmma);
            }
        }

        // Ignore blocks when establishing the next index of preceding
        // activities. We only want 'nextIndex' fields to be updated to
        // point to concrete activities.
        if (!(act instanceof Block)) {
            establishNextIndex();
        }
    }
    
    /**
     * This method processes the end of an activity.
     * 
     * @param act The activity
     */
    protected void endActivity(Activity act) {
        
        if (act.getParent() instanceof  org.scribble.protocol.model.Choice) {
            java.util.List<Object> cache=getCache(act.getParent());
            
            cache.addAll(_pendingNextIndex);
        }
        
        // Check if block associated with a parallel or an activity in an
        // unordered construct
        if (act.getParent() instanceof  org.scribble.protocol.model.Choice
                || act.getParent() instanceof  org.scribble.protocol.model.Parallel
                || (act.getParent() instanceof Block
                        && act.getParent().getParent() instanceof Unordered)) {
            
            // Make sure pending 'nextIndex' nodes do not get
            // set - they just need to result in the trail
            // for that context being completed so that the
            // parent context is reactivated (once all other
            // paths have equally finished)
            _pendingNextIndex.clear();
        }
    }
    
    /**
     * This method returns a parallel node (if appropriate) for the supplied
     * activity node.
     * 
     * @param act The activity
     * @return The parallel node or null if not relevant
     */
    protected org.scribble.protocol.monitor.model.Parallel getParallelNode(Activity act) {
        org.scribble.protocol.monitor.model.Parallel ret=null;
        
        if (act instanceof Block && act.getParent() instanceof org.scribble.protocol.model.Parallel) {
            ret = (org.scribble.protocol.monitor.model.Parallel)_nodeMap.get(act.getParent());
        } else if (act.getParent() instanceof Block
                && act.getParent().getParent() instanceof Unordered) {
            ret = (org.scribble.protocol.monitor.model.Parallel)_nodeMap.get(act.getParent().getParent());
        }
    
        return (ret);
    }
            
    /**
     * This method indicates the end of a
     * block.
     * 
     * @param elem The block
     */
    public void end(Block elem) {

        endActivity(elem);
        
        /*
        // Check if block associated with a parallel
        if (elem.getParent() instanceof Parallel) {
            
            // Make sure pending 'nextIndex' nodes do not get
            // set - they just need to result in the trail
            // for that context being completed so that the
            // parent context is reactivated (once all other
            // paths have equally finished)
            m_pendingNextIndex.clear();
        }
        */
    }
    
    /**
     * This method visits an import component.
     * 
     * @param elem The import
     */
    public void accept(TypeImportList elem) {
    }
    
    /**
     * This method visits an import component.
     * 
     * @param elem The import
     */
    public void accept(ProtocolImportList elem) {
    }
    
    /**
     * This method visits the introduces construct.
     * 
     * @param elem The introduces construct
     */
    public void accept(Introduces elem) {
    }
    
    /**
     * This method visits an interaction component.
     * 
     * @param elem The interaction
     */
    public void accept(Interaction elem) {
        startActivity(elem);
        
        createInteraction(elem.getMessageSignature(), elem.getFromRole(), elem.getToRoles(),
                            elem.getAnnotations());
        
        endActivity(elem);
    }

    /**
     * This method creates an interaction node.
     * 
     * @param ms The message signature
     * @param fromRole The from role
     * @param toRoles The to roles
     * @param annotations The annotations
     */
    protected void createInteraction(MessageSignature ms, Role fromRole,
                            java.util.List<Role> toRoles,
                            java.util.List<org.scribble.common.model.Annotation> annotations) {
        
        // TODO: Think about how best to set the next index. Can only do this
        // if the next node is within the same scope. If (for example) the
        // last node in a nested-protocol, which happens to be called from
        // multiple locations, then we need to create a sub-conversation
        // context with the positions - and when the trail ends, it should
        // complete the sub-conversation context, causing the parent
        // context to be notified with the appropriate continuation point.
        
        MessageNode node=null;
        
        if (toRoles.size() > 0) {
            node = new SendMessage();
        } else {
            node = new ReceiveMessage();
        }
        
        if (ms.getOperation() != null) {
            node.setOperator(ms.getOperation());
        }
        
        for (TypeReference tref : ms.getTypeReferences()) {
            String type=tref.getName();
            
            // Get type import associated with the reference
            TypeImport ti=TypesUtil.getTypeImport(tref);
            
            if (ti != null && ti.getDataType() != null) {
                type = ti.getDataType().getDetails();
                
            } else if (TypesUtil.isConcreteTypesDefined(tref.getModel())) {
                // Check if any concrete types have bene defined
                // If so, report failure to resolve concrete type
                _journal.error("Concrete type not found for '"
                            +type+"'", tref.getProperties());
            }
                        
            MessageType mt=new MessageType();
            mt.setValue(type);
            node.getMessageType().add(mt);
        }
        
        // Export annotations
        createAnnotations(node, annotations);
        
        _nodes.add(node);
                    
        _pendingNextIndex.add(node);
    }
    
    /**
     * This method creates annotations on the supplied node.
     * 
     * @param node The node
     * @param annotations The annotations
     */
    protected void createAnnotations(Node node,
            java.util.List<org.scribble.common.model.Annotation> annotations) {
        
        for (org.scribble.common.model.Annotation pma : annotations) {
            org.scribble.protocol.monitor.model.Annotation pmma=
                        new org.scribble.protocol.monitor.model.Annotation();
            
            if (pma.getId() != null) {
                pmma.setId(pma.getId());
            } else {
                pmma.setId(UUID.randomUUID().toString());
            }
            pmma.setValue(pma.toString());
            
            node.getAnnotation().add(pmma);
        }
    }
    
    /**
     * This method returns the current node index.
     * 
     * @return The index
     */
    protected int getCurrentIndex() {
        return (_nodes.size());
    }
    
    /**
     * This method establishes the next index.
     */
    protected void establishNextIndex() {
        // Establish next index association with this node
        establishNextIndex(getCurrentIndex());
    }
    
    /**
     * This method establishes the next index.
     * 
     * @param pos The position
     */
    protected void establishNextIndex(int pos) {
        for (Object b : _pendingNextIndex) {
            
            if (b instanceof Node) {
                ((Node)b).setNextIndex(pos);
            } else if (b instanceof Path) {
                ((Path)b).setNextIndex(pos);
            }
        }
        
        _pendingNextIndex.clear();
    }
    
    /**
     * This method indicates the start of a
     * protocol.
     * 
     * @param elem The protocol
     * @return Whether to process the contents
     */
    @Override
    public boolean start(org.scribble.protocol.model.Protocol elem) {
        // Clear pending next index
        _pendingNextIndex.clear();
        
        // Cache the current node position associated with the protocol
        _nodeMap.put(elem, new Integer(_nodes.size()));
        
        return (true);
    }
    
    /**
     * This method indicates the end of a
     * protocol.
     * 
     * @param elem The protocol
     */
    @Override
    public void end(org.scribble.protocol.model.Protocol elem) {
        // Clear pending next index
        _pendingNextIndex.clear();
        
        // Check if position has moved, and if so, load the nodes associated
        // with the run activities for this protocol
        Integer pos=(Integer)_nodeMap.get(elem);
        
        if (pos != null && pos.intValue() < _nodes.size()) {
            
            java.util.List<Object> objs=_nodeCache.get(elem);
            
            for (int i=0; objs != null && i < objs.size(); i++) {
                if (objs.get(i) instanceof Decision) {
                    ((Decision)objs.get(i)).setInnerIndex(pos.intValue());
                } else if (objs.get(i) instanceof Scope) {
                    ((Scope)objs.get(i)).setInnerIndex(pos.intValue());                        
                }
            }
        }
    }
    
    /**
     * This method indicates the start of a
     * choice.
     * 
     * @param elem The choice
     * @return Whether to process the contents
     */
    public boolean start(org.scribble.protocol.model.Choice elem) {

        startActivity(elem);
        
        org.scribble.protocol.monitor.model.Choice node=
                new org.scribble.protocol.monitor.model.Choice();
        
        _nodes.add(node);
                    
        // Cache the node associated with the choice
        _nodeMap.put(elem, node);

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * choice.
     * 
     * @param elem The choice
     */
    public void end(org.scribble.protocol.model.Choice elem) {
        
        java.util.List<Object> cache=getCache(elem);

        _pendingNextIndex.addAll(cache);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * directed choice.
     * 
     * @param elem The choice
     * @return Whether to process the contents
     */
    public boolean start(org.scribble.protocol.model.DirectedChoice elem) {

        startActivity(elem);
        
        org.scribble.protocol.monitor.model.Choice node=
                new org.scribble.protocol.monitor.model.Choice();
        
        _nodes.add(node);
                    
        // Cache the node associated with the choice
        _nodeMap.put(elem, node);

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * directed choice.
     * 
     * @param elem The choice
     */
    public void end(org.scribble.protocol.model.DirectedChoice elem) {
        
        java.util.List<Object> cache=getCache(elem);

        _pendingNextIndex.addAll(cache);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * directed choice.
     * 
     * @param elem The choice
     * @return Whether to process the contents
     */
    public boolean start(org.scribble.protocol.model.OnMessage elem) {

        DirectedChoice dc=(DirectedChoice)elem.getParent();
        
        // Create path node
        Path path=new Path();
        
        // Define id associated with the choice label
        //path.setId(ChoiceUtil.getLabel(elem.getMessageSignature()));
        
        org.scribble.protocol.monitor.model.Choice choiceBuilder=
            (org.scribble.protocol.monitor.model.Choice)_nodeMap.get(elem.getParent());

        java.util.List<Path> pathBuilderList=
                    _choicePaths.get(choiceBuilder);
        
        if (pathBuilderList == null) {
            pathBuilderList = new java.util.Vector<Path>();
            _choicePaths.put(choiceBuilder, pathBuilderList);
        }
        
        pathBuilderList.add(path);
        
        _pendingNextIndex.add(path);

        // Create annotations
        for (org.scribble.common.model.Annotation pma : elem.getAnnotations()) {
            org.scribble.protocol.monitor.model.Annotation pmma=
                        new org.scribble.protocol.monitor.model.Annotation();
            
            if (pma.getId() != null) {
                pmma.setId(pma.getId());
            } else {
                pmma.setId(UUID.randomUUID().toString());
            }
            pmma.setValue(pma.toString());
            
            path.getAnnotation().add(pmma);
        }

        establishNextIndex();

        // Create interaction associated with message signature
        createInteraction(elem.getMessageSignature(), dc.getFromRole(), dc.getToRoles(),
                elem.getAnnotations());

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * directed choice.
     * 
     * @param elem The choice
     */
    public void end(org.scribble.protocol.model.OnMessage elem) {
        // Transfer outstanding nodes in 'pendingNextIndex' to the
        // cache associated with the choice
        java.util.List<Object> cache=getCache(elem.getParent());

        cache.addAll(_pendingNextIndex);

        _pendingNextIndex.clear();
    }
    
    /**
     * This method retrieves a cache of objects associated with
     * the supplied model object.
     * 
     * @param elem The model object
     * @return The list of cached objects
     */
    protected java.util.List<Object> getCache(ModelObject elem) {
        java.util.List<Object> ret=_nodeCache.get(elem);
        
        if (ret == null) {
            ret = new java.util.Vector<Object>();
            _nodeCache.put(elem, ret);
        }
        
        return (ret);
    }
    
    /**
     * This method visits a run component.
     * 
     * @param elem The run
     */
    public void accept(org.scribble.protocol.model.Run elem) {
        
        startActivity(elem);

        org.scribble.protocol.monitor.model.Scope node=
                    new org.scribble.protocol.monitor.model.Scope();
        
        _nodes.add(node);
                    
        // Cache the node associated with the run
        _nodeMap.put(elem, node);
        
        // Check if calling a local protocol
        org.scribble.protocol.model.Protocol inner=
                RunUtil.getInnerProtocol(elem.getEnclosingProtocol(), elem.getProtocolReference());
        
        if (inner != null) {
            java.util.List<Object> cache=_nodeCache.get(inner);
            
            if (cache == null) {
                cache = new java.util.Vector<Object>();
                _nodeCache.put(inner, cache);
            }
            
            cache.add(node);
        }
        
        /*
        if (elem.isInline()) {
            m_pendingNextIndex.add(node);
        }
        */

        //Node node=
        //        (Node)m_nodeMap.get(elem);
            
        /*
        if (elem.isInline()) {
            m_pendingNextIndex.clear();
            
            if (node instanceof Scope) {
                ((Scope)node).setInnerIndex(m_nodes.indexOf(node)+1);
            }
        }
        */
        
        _pendingNextIndex.add(node);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * parallel.
     * 
     * @param elem The parallel
     * @return Whether to process the contents
     */
    public boolean start(org.scribble.protocol.model.Parallel elem) {
        
        startActivity(elem);

        org.scribble.protocol.monitor.model.Parallel node=
                    new org.scribble.protocol.monitor.model.Parallel();
        
        _nodes.add(node);
                    
        // Cache the node associated with the parallel
        _nodeMap.put(elem, node);

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * parallel.
     * 
     * @param elem The parallel
     */
    public void end(org.scribble.protocol.model.Parallel elem) {
        Node node=
            (Node)_nodeMap.get(elem);
        
        _pendingNextIndex.add(node);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * Unordered.
     * 
     * @param elem The Unordered
     * @return Whether to process the contents
     */
    public boolean start(Unordered elem) {
        
        startActivity(elem);

        org.scribble.protocol.monitor.model.Parallel node=
                    new org.scribble.protocol.monitor.model.Parallel();
        
        _nodes.add(node);
                    
        // Cache the node associated with the parallel
        _nodeMap.put(elem, node);

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * Unordered.
     * 
     * @param elem The Unordered
     */
    public void end(Unordered elem) {
        Node node=
            (Node)_nodeMap.get(elem);
        
        _pendingNextIndex.add(node);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * repeat.
     * 
     * @param elem The repeat
     * @return Whether to process the contents
     */
    public boolean start(Repeat elem) {        

        startActivity(elem);

        Decision node=new Decision();
        
        _nodes.add(node);
                    
        // Cache the node associated with the choice
        _nodeMap.put(elem, node);

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * repeat.
     * 
     * @param elem The repeat
     */
    public void end(Repeat elem) {    
        Node node=
            (Node)_nodeMap.get(elem);
        
        // Set the current 'pending next index' entries to the
        // repeat node
        establishNextIndex(_nodes.indexOf(node));
        
        if (node instanceof Decision) {
            ((Decision)node).setInnerIndex(_nodes.indexOf(node)+1);
        }
        
        _pendingNextIndex.add(node);
        
        endActivity(elem);
    }
    
    /**
     * This method visits a recursion component.
     * 
     * @param elem The recursion
     */
    public void accept(Recursion elem) {

        startActivity(elem);
        
        Call node=new Call();

        if (_recurPosition.containsKey(elem.getLabel())) {
            //establishNextIndex(m_recurPosition.get(elem.getLabel()));
            
            node.setCallIndex(_recurPosition.get(elem.getLabel()));
        } else {
            LOG.severe("Unable to find recursion label '"+elem.getLabel()+"'");
        }

        _nodes.add(node);                    

        _pendingNextIndex.add(node);
        
        // TODO: Not sure how this should be handled when in unordered construct??
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * labelled block.
     * 
     * @param elem The labelled block
     * @return Whether to process the contents
     */
    public boolean start(RecBlock elem) {
        
        startActivity(elem);
        
        // Register current node against labelled block label
        _recurPosition.put(elem.getLabel(), _nodes.size());

        org.scribble.protocol.monitor.model.Scope node=
            new org.scribble.protocol.monitor.model.Scope();
        
        _nodes.add(node);
                    
        // Cache the node associated with the run
        _nodeMap.put(elem, node);
        
        _pendingNextIndex.add(node);
        
        return (true);
    }
    
    /**
     * This method indicates the end of a
     * labelled block.
     * 
     * @param elem The labelled block
     */
    public void end(RecBlock elem) {
        Node node=
            (Node)_nodeMap.get(elem);
        
        _pendingNextIndex.clear();
        
        if (node instanceof Scope) {
            ((Scope)node).setInnerIndex(_nodes.indexOf(node)+1);
        }

        _pendingNextIndex.add(node);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * try escape.
     * 
     * @param elem The try escape
     * @return Whether to process the contents
     */
    public boolean start(org.scribble.protocol.model.Do elem) {
        
        startActivity(elem);

        org.scribble.protocol.monitor.model.Do node=
                new org.scribble.protocol.monitor.model.Do();
        
        _nodes.add(node);
                    
        // Cache the node associated with the try
        _nodeMap.put(elem, node);
        
        return (true);
    }
    
    /**
     * This method indicates the end of a
     * try escape.
     * 
     * @param elem The try escape
     */
    public void end(org.scribble.protocol.model.Do elem) {
        Node node=
            (Node)_nodeMap.get(elem);
        
        _pendingNextIndex.clear();
        
        if (node instanceof Scope) {
            ((Scope)node).setInnerIndex(_nodes.indexOf(node)+1);
        }

        _pendingNextIndex.add(node);
        
        endActivity(elem);
    }
    
    /**
     * This method indicates the start of a
     * try escape.
     * 
     * @param elem The try escape
     * @return Whether to process the contents
     */
    public boolean start(Interrupt elem) {
        org.scribble.protocol.monitor.model.Do node=
                (org.scribble.protocol.monitor.model.Do)_nodeMap.get(elem.getParent());
        
        _pendingNextIndex.clear();
        
        Path path=new Path();
        path.setNextIndex(getCurrentIndex());
        
        node.getPath().add(path);
        
        // Create annotations
        for (org.scribble.common.model.Annotation pma : elem.getAnnotations()) {
            org.scribble.protocol.monitor.model.Annotation pmma=
                        new org.scribble.protocol.monitor.model.Annotation();
            
            if (pma.getId() != null) {
                pmma.setId(pma.getId());
            } else {
                pmma.setId(UUID.randomUUID().toString());
            }
            pmma.setValue(pma.toString());
            
            path.getAnnotation().add(pmma);
        }

        return (true);
    }
    
    /**
     * This method indicates the end of a
     * try escape.
     * 
     * @param elem The try escape
     */
    public void end(Interrupt elem) {
        // End of catch block scope, so clear the 'pending next index' nodes
        _pendingNextIndex.clear();
    }
    
    /**
     * This method visits a custom activity component.
     * 
     * @param elem The custom activity
     */
    public void accept(CustomActivity elem) {
    }
    
    /**
     * This method returns the protocol description exported from the visited ProtocolModel.
     * 
     * @return The protocol description
     */
    public org.scribble.protocol.monitor.model.Description getDescription() {
        Description ret=new Description();
        
        // Initialise the choice paths. This is done here, rather than in the
        // endWhen or endChoice, as the nextIndex on a when path may not be
        // initialised until outside the choice.
        for (org.scribble.protocol.monitor.model.Choice choice : _choicePaths.keySet()) {
            java.util.List<Path> pathBuilders=_choicePaths.get(choice);
            
            for (Path pathBuilder : pathBuilders) {
                choice.getPath().add(pathBuilder);
            }
        }

        // Same reason as for choice
        for (org.scribble.protocol.monitor.model.Parallel parallel : _parallelPaths.keySet()) {
            java.util.List<Path> pathBuilders=_parallelPaths.get(parallel);
            
            for (Path pathBuilder : pathBuilders) {
                parallel.getPath().add(pathBuilder);
            }
        }

        for (Node b : _nodes) {
            ret.getNode().add(b);
        }
        
        return (ret);
    }
}
