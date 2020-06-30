/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.AssemblerCreationException;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class ResultsHelper {
	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			Callback callback,
			RowTransformer<R> rowTransformer,
			JdbcValues jdbcValues) {
		final Map<NavigablePath,Initializer> initializerMap = new LinkedHashMap<>();
		final List<Initializer> initializers = new ArrayList<>();

		//noinspection rawtypes
		final List<DomainResultAssembler> assemblers = jdbcValues.getValuesMapping().resolveAssemblers(
				new AssemblerCreationState() {
					@Override
					public Initializer resolveInitializer(
							NavigablePath navigablePath,
							Supplier<Initializer> producer) {
						final Initializer existing = initializerMap.get( navigablePath );
						if ( existing != null ) {
							ResultsLogger.LOGGER.debugf(
									"Returning previously-registered initializer : %s",
									existing
							);
							return existing;
						}

						final Initializer initializer = producer.get();
						ResultsLogger.LOGGER.debugf(
								"Registering initializer : %s",
								initializer
						);

						initializerMap.put( navigablePath, initializer );
						initializers.add( initializer );

						return initializer;
					}

					@Override
					public SqlAstCreationContext getSqlAstCreationContext() {
						return sessionFactory;
					}
				}
		);

		return new StandardRowReader<>(
				assemblers,
				initializers,
				rowTransformer,
				callback
		);
	}

	public static void finalizeCollectionLoading(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection collectionInstance,
			Object key) {
		CollectionEntry collectionEntry = persistenceContext.getCollectionEntry( collectionInstance );
		if ( collectionEntry == null ) {
			collectionEntry = persistenceContext.addInitializedCollection(
					collectionDescriptor,
					collectionInstance,
					key
			);
		}
		else {
			collectionEntry.postInitialize( collectionInstance );
		}

		if ( collectionDescriptor.getCollectionType().hasHolder() ) {
			persistenceContext.addCollectionHolder( collectionInstance );
		}

		final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();
		batchFetchQueue.removeBatchLoadableCollection( collectionEntry );

		// todo (6.0) : there is other logic still needing to be implemented here.  caching, etc
		// 		see org.hibernate.engine.loading.internal.CollectionLoadContext#endLoadingCollection in 5.x
	}

	public static Fetch extractElementFetch(List<Fetch> buildFetches) {
		if ( buildFetches == null || buildFetches.size() != 1 ) {
			throw new AssemblerCreationException( "Could not build element fetch" );
		}

		return buildFetches.get( 0 );
	}
}
